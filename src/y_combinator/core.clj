(ns y-combinator.core)

;; Standard definition of factorial

(def fact
  (fn [n] (if (zero? n) 1 (* n (fact (dec n))))))

(fact 5)

;; Inline function definition (in lambda-calc there are no assignments)

(ns-unmap 'y-combinator.core 'fact)

((fn [n] (if (zero? n) 1 (* n (fact (dec n))))) 5)

;; As fact is not defined, create a function which takes fact as an argument

(def make-fact
  (fn [fact]
    (fn [n] (if (zero? n) 1 (* n (fact (dec n)))))))

(make-fact :???)

;; We relax the parameter condition to demand a full factorial function and we don't
;; demand a factorial function as parameter

(def improver
  (fn [partial]
    (fn [n] (if (zero? n) 1 (* n (partial (dec n)))))))

(def error (fn [n] (throw (RuntimeException. "SHOULD NOT BE CALLED"))))

(def f0 (improver error))
(def f1 (improver f0))
(def f2 (improver f1))
(def f3 (improver f2))

;; We have improved the function to compute factorials upto 3

(println (map f3 [0 1 2 3]))

;; but 4 fails

;(f3 4)

;; We can inline the applications of improver

(def fx (improver
          (improver
            (improver
              (improver
                (improver
                  (improver
                    (improver
                      (improver
                        (improver
                          (improver
                            (improver
                              (improver
                                (improver
                                  (improver
                                    (improver
                                      (improver error)))))))))))))))))

(fx 15)

;; We can do the same trick as before and pass improver as a parameter of a function

(def fx ((fn [improver] (improver (improver (improver error))))
         (fn [partial]
           (fn [n] (if (zero? n) 1 (* n (partial (dec n))))))))

(map fx [0 1 2])

;; Let's do something radical and different: instead of passing a "normal" function to be improved to improver
;; pass improver to itself
;; - We rename partial to improver, because it is what we are passing to it
;; - We change the partial call to (improver improver) bacause the function no longer accept a number but an
;; improver function

(def fx ((fn [improver] (improver improver))
         (fn [improver]
           (fn [n] (if (zero? n) 1 (* n ((improver improver) (dec n))))))))

;; fx is the true factorial !!!!

(map fx (range 20))

;; Let's get rid of the assignement by inlining the function body in (fx 5)

(((fn [improver] (improver improver))
  (fn [improver]
    (fn [n] (if (zero? n) 1 (* n ((improver improver) (dec n)))))))
  5)

;; (This is a full lambda-calculues expression of factorial of 5.)


;; Let's rename improver by the name used in Y-combinator literature

(((fn [x] (x x))
  (fn [x]
    (fn [n] (if (zero? n)  1 (* n ((x x) (dec n)))))))
 5)

;; Let's do Tennent's correspondece principle: (fn [n] e) == (fn [n] ((fn [] e)))

(((fn [x] (x x))
  (fn [x]
    ((fn []
       (fn [n] (if (zero? n) 1 (* n ((x x) (dec n)))))))))
 5)

;; Let's wrap (x x) in a function (it is needed to avoid stack overflow in an applicative order language)

(((fn [x] (x x))
  (fn [x]
    ((fn []
       (fn [n] (if (zero? n) 1 (* n ((fn [v] ((x x) v)) (dec n)))))))))
 5)

;; Let's introduce a binding and pass error as parameter (it can be anything as it is not used)

(((fn [x] (x x))
  (fn [x]
    ((fn [code]
       (fn [n] (if (zero? n) 1 (* n ((fn [v] ((x x) v)) (dec n))))))
      error)))
 5)

;; Let's change error by (fn [v] ((x x) v))

(((fn [x] (x x))
  (fn [x]
    ((fn [code]
       (fn [n] (if (zero? n) 1 (* n ((fn [v] ((x x) v)) (dec n))))))
     (fn [v] ((x x) v)))))
 5)

;; I can un-inline the inner (fn [v] ((x x) v))

(((fn [x] (x x))
  (fn [x]
    ((fn [code]
       (fn [n] (if (zero? n) 1 (* n (code (dec n))))))
     (fn [v] ((x x) v)))))
 5)

;; Let's rename code to partial and, voila, the improver fuunction reappears !!

(((fn [x] (x x))
  (fn [x]
    ((fn [partial]
       (fn [n] (if (zero? n) 1 (* n (partial (dec n))))))
     (fn [v] ((x x) v)))))
 5)

;; Let's try to pull the function to the outside. Let's do a Tennent's correspondence

(((fn []
    ((fn [x] (x x))
     (fn [x]
       ((fn [partial]
           (fn [n] (if (zero? n) 1 (* n (partial (dec n))))))
        (fn [v] ((x x) v)))))))
 5)

;; Let's introduce a parameter

(((fn [code]
    ((fn [x] (x x))
     (fn [x]
       ((fn [partial]
          (fn [n] (if (zero? n) 1 (* n (partial (dec n))))))
        (fn [v] ((x x) v))))))
  error)
 5)

;; Let's substitute error by the factorial improver buried inside

(((fn [code]
    ((fn [x] (x x))
     (fn [x]
        ((fn [partial]
           (fn [n] (if (zero? n) 1 (* n (partial (dec n))))))
         (fn [v] ((x x) v))))))
  (fn [partial]
    (fn [n] (if (zero? n) 1 (* n (partial (dec n)))))))
 5)

;; Let's un-inline the improver

(((fn [code]
    ((fn [x] (x x))
     (fn [x] (code (fn [v] ((x x) v))))))
  (fn [partial]
    (fn [n] (if (zero? n) 1 (* n (partial (dec n)))))))
 5)

;; And let's call code improver, as I'm passing the factorial improver to it

(((fn [improver]
    ((fn [x] (x x))
     (fn [x] (improver (fn [v] ((x x) v))))))
  (fn [partial]
    (fn [n] (if (zero? n) 1 (* n (partial (dec n)))))))
 5)

;; Let's arrange the things

(def factorial-improver
  (fn [partial]
    (fn [n] (if (zero? n) 1 (* n (partial (dec n)))))))

(((fn [improver]
    ((fn [x] (x x))
     (fn [x] (improver (fn [v] ((x x) v))))))
  factorial-improver)
 5)

;; A little bit more

(def factorial-improver
  (fn [partial]
    (fn [n] (if (zero? n) 1 (* n (partial (dec n)))))))

(def Y
  (fn [improver]
    ((fn [x] (x x))
     (fn [x] (improver (fn [v] ((x x) v)))))))

;; Y calculates the fixed-point of an improver function

(def factorial (Y factorial-improver))

(factorial 5)


;; We change the name of the improver function to f

(def Y
  (fn [f]
    ((fn [x] (x x))
     (fn [x] (f (fn [v] ((x x) v)))))))

;; As (x x) returns the full factorial function we can improve into it and obtain the same function so we can
;; replace (x x) by (f (x x))

(def Y
  (fn [f]
    ((fn [x] (f (x x)))
     (fn [x] (f (fn [v] ((x x) v)))))))

;; And then we can introduce an indirection

;; Fixpoint Combinator
;; Applicative Order Y-Combinator
;; Z-Combinator

(def Y
  (fn [f]
    ((fn [x] (f (fn [v] ((x x) v))))
     (fn [x] (f (fn [v] ((x x) v)))))))
