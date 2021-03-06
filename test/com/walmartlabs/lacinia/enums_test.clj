(ns com.walmartlabs.lacinia.enums-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [com.walmartlabs.test-schema :refer [test-schema]]
    [com.walmartlabs.lacinia :refer [execute]]
    [com.walmartlabs.lacinia.schema :as schema])
  (:import
    (clojure.lang ExceptionInfo)))

(def ^:dynamic *compiled-schema* nil)

(use-fixtures :once
  (fn [f]
    (binding [*compiled-schema* (schema/compile test-schema)]
      (f))))

(defn q [query]
  (execute *compiled-schema* query nil nil))

(deftest can-provide-enum-as-bare-name
  (is (= "Luke Skywalker"
         (-> "{ hero(episode: NEWHOPE) { name }}"
             q
             :data
             :hero :name))))

(deftest handling-of-invalid-enum-value
  (let [result (q "{ hero (episode: CLONES) { name }}")
        errors (-> result :errors)
        first-error (first errors)]
    (is (-> result (contains? :data) not))
    (is (= 1 (count errors)))
    (is (= {:allowed-values #{"EMPIRE"
                              "JEDI"
                              "NEWHOPE"}
            :argument :episode
            :enum-type :episode
            :field :hero
            :locations [{:column 0
                         :line 1}]
            :message "Exception applying arguments to field `hero': For argument `episode', provided argument value is not member of enum type."
            :query-path []
            :value "CLONES"}
           first-error))))

(deftest enum-values-must-be-unique
  (let [e (is (thrown? ExceptionInfo
                       (schema/compile {:enums {:invalid {:values [:yes 'yes "yes"]}}})))]
    (is (= (.getMessage e)
           "Values defined for enum `invalid' must be unique."))
    (is (= {:enum {:values [:yes 'yes "yes"]
                   :category :enum
                   :type-name :invalid}}
           (ex-data e)))))
