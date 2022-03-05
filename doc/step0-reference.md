# Clojure + Reference

This 'step' will really just be about putting all the bits into a place where you would come back and quickly reference things like 
syntax, libraries used and some level of glossary.

Feel free to skim this section, it's about having an achor as you work through the rest of the material in this repository.

I'll add a bunch of reference links here first, and then go on a bit of an 'intro to clojure' rant.

## Links used through this project:

In roughly the order given.

### Step 0

- Clojure - The Reader: https://clojure.org/reference/reader
- Clojure cheatsheet: https://clojure.org/api/cheatsheet
- ClojureScript cheatsheet: https://cljs.info/cheatsheet/
- Common Libraries: https://www.clojure-toolbox.com/


### Step 1

- Step 1: https://github.com/yuppiechef/yc-bootstrap/tree/step1-initial
- Clojure - Getting Started: https://clojure.org/guides/getting_started
- Clojure - Deps and CLI: https://clojure.org/guides/deps_and_cli

### Step 2

- Step 2: https://github.com/yuppiechef/yc-bootstrap/tree/step2-uberjar
- Uberdeps: https://github.com/tonsky/uberdeps

### Step 3

- Step 3: https://github.com/yuppiechef/yc-bootstrap/tree/step3-cleanup
- nRepl: https://github.com/nrepl/nrepl

### Step 4

- Step 4: https://github.com/yuppiechef/yc-bootstrap/tree/step4-http
- Http-kit: https://github.com/http-kit/http-kit
- Server Doc: https://http-kit.github.io/server.html#stop-server

### Step 5

- Step 5: https://github.com/yuppiechef/yc-bootstrap/tree/step5-hotreloading
- Hawk: https://github.com/wkf/hawk

### Step 6

- Step 6: https://github.com/yuppiechef/yc-bootstrap/tree/step6-static-files
- Compojure: https://github.com/weavejester/compojure
- Favicon: https://favicon.io/emoji-favicons/
- Favicon - cat with wry smile: https://favicon.io/emoji-favicons/cat-with-wry-smile
- Ring: https://github.com/ring-clojure/ring
- ring.util.response: https://github.com/ring-clojure/ring/blob/master/ring-core/src/ring/util/response.clj

### Step 7

- Step 7: https://github.com/yuppiechef/yc-bootstrap/tree/step7-clojurescript 
- ClojureScript Quick Start: https://clojurescript.org/guides/quick-start
- Shadow-cljs User Guide: https://shadow-cljs.github.io/docs/UsersGuide.html
- Shadow-cljs Standalone Code via NPM: https://shadow-cljs.github.io/docs/UsersGuide.html#_standalone_via_code_npm_code
- NPM Installing Node and NPM: https://docs.npmjs.com/downloading-and-installing-node-js-and-npm


### Step 8

- Step 8: https://github.com/yuppiechef/yc-bootstrap/tree/step8-rum-ssr
- Rum: https://github.com/tonsky/rum


## Clojure

### What it is:

Clojure is a pragmatic, hosted, dynamic & functional lisp programming language. Let's unpack that:

*Pragmatic*: The language is designed to be for real world work, so if there are conflicting ideals (like functional purity), the design selects the pragmatic approach.

*Hosted*: There is no clojure 'native' VM from scratch (though there absolutely could be). The focus of clojure is not to reinvent the wheel outside of its core focus. 

Re-inventing a runtime or trying to shoehorn a whole new language in a browser would require a lot of energy for no tangible benefit. Primary examples where Clojure plays is on the JVM and in Javascript (browser or Node)

*Dynamic*: There is no static type-checking present in the core of Clojure - there is support for this provided by libraries, but usually you'll want to apply that in specific cases where it makes pragmatic sense.

This may sound terrifying if you're coming from a statically checked language, but in reality the feedback loop of that Clojure encourages lets you reason more accurately about your code, and in local scope, so it's less of a concern that one might assume. It's just different.

*Functional*: Primarily around the focus on efficiently immutable datastructures from the ground up - focussing on functions being the primary unit of work (as opposed to collecting methods and properties of objects together as OOP does).

Pure functions are a part of this, and while it does make your programs simpler to reason about, that's not the driving force here.

### Why should I care:

It's yet another language, right? Yes, it is. But working and understanding the principle values that is foundational to Clojure will strongly influence the way that you think about coding. A lot of this thinking is becoming more mainstream (this is great!) - but it's still worthwhile to understand.

Clojure is fun! I think this boils down to two things:
 - The tendency toward tight feedback loops - you always feel like you're chipping away at your problem at hand and (once you have a grasp of the language) you find that your thinking is centered around your problem space, as opposed to wrestling with the language.
 - Reliability - When you write your functions and you've confirmed they're working as you expect, they tend to just keep working as you expect, pretty much infinitely. This let's you get back to your problem space (once again) instead of wrestling at the coding level forever.

 ### Syntax

To reason about Clojure, it is important to divide the learning into two distinct parts: Understanding the fundamental data structures ('syntax') of the language, and the seperate distinct step of evaluating those data structures for the evaluator do execute the code.

A particularly popular way of interacting with Clojure in the beginning is to use the REPL. (you'll see this _everywhere_). This simply means Read, Eval, Print, Loop:

*Read*: Read the input form and convert into in-memory data structure.
*Eval*: Evaluate that data structure (possibly with side effects), using a specific ruleset
*Print*: Print out the result of the evaluated data structure.
*Loop*: Start at beginning.

#### Read

For ease, we divide the 'read' into two parts: base values and composite values, though fundamentally, they're all just values.

##### Literal values

https://clojure.org/reference/reader

```clojure
; Comments start from semi-colon
;; OR double semi-colon, for full line comments, just a convention

; null:
nil ; None, empty value. Source of much pain.

; boolean
true false ; Very simple. Only `false` and `nil` are considered false in Clojure, every single other value (including collections, even if empty) are considered true.

; Numbers:
1 2 3.2 5.0f 2; Just as you'd expect, the 3.2 will be read as a double, the `f` and `L` suffix indicate floating point numbers. 
2r101010 052 8r52 0x2a 36r16 42 ; are all the same Long represented with base 2 radix, octal, octal (specified radix 8), hexadecimal and base 36 (maximum base) respectively. 
4M 4N ; BigDecimal or BigInteger, respectively. Don't catch fright, these just support large numbers and Clojure sometimes will switch to these if there would be an overflow when using the core math functions.

; Ratios:
2/3 ; actually represented like this, it allows lossless arithmetic - can be surprised though, since you don't see it often.

; Characters:
\h \e \l \0 ; The backslash precedes actual characters. Unlike many languages where single quotes are used, like 'h'.

; Strings:
"Hel\r\n0" ; Strings are fundamentally simply a list of characters. You can include escape characters with the backslash 

; Regex:
#"[0-9]" ; These compile down to platform native regular expressions used for search matching (a single number between zero and nine in this case)

; Whitespace
, ; comma's are treated entirely equivalent to whitespace (no effect), so you can use it to format your code if you like. More seasoned Clojure developers simply drop it entirely though.

; Symbols
fred x get->thingy1 util/add ; These are examples of straight symbols in Clojure - specifically: Symbols begin with a non-numeric character and can contain alphanumeric characters and *, +, !, -, _, ', ?, <, > and =.
; The `/` is special in that it provides a 'namespace' for the symbol.

; Symbolic values:
##Inf ##-Inf ##NaN ; Positive and Negative infinity, and Not a Number, respectively.

; Keywords:

:fred :x :get->thingy :util/add ; Effectively strings that are more easily readible and optimized for indexing and lookup - they can have a namespace part with the `/` character.
::foo ; will resolve to having the current namespace (so in `user` namespace it will be equivalent to `:user/foo`)

```

Refer to docs for more in-depth and specific explanations.

##### Collection values

*Lists*

```clojure
(1 2 3 4) ; List with 4 elements

(+ 1 2) ; List with 3 elements

(fred :bob "Mary") ; Note, you can mix any types of values

(this (is a) (:nested "List")) ; Like other lists
```

Lists are denoted with plain parentheses `()`, spaces between elements (comma's are whitespace). 

As demonstrated above, the types of the values in each element of a list doesn't matter. This is true of all the collection in Clojure

Importantly, lists are used by the evaluator of Clojure to mark out function calls -though you can coerce it not to by prepending a `'` quote, like:

```clojure
`(+ 1 2) ; Also a list, but the clojure evaluator will not try call this `+` as a function, so you get a literal list back.
```

They are immutable, which means that Clojure can structurally share common tails. ie:

```clojure
(let [x (1 2 3)
      y (conj 4 x) ; Create a new list, adding 4 as an element to x
      z (conj 5 x)])
```

In the above, `x` will be `(1 2 3)`, `y` will be `(4 1 2 3)` and `z` will be `(5 1 2 3)` - but instead of _copying_ `(1 2 3)` to each list, `y` is represented as `4` at the head and `x` at the tail, and similarly for `z`. This is possible due to there being no chance that the elements in `x` can change.

Strictly, they are singly linked lists, so as mentioned above, a list is effectively made of two parts, a `head` and a `tail`. The `head` is the first value of the list and the `tail` refers to the next head/tail pair. `nil` tail means end of list.

In more conventional terminology, the head/tail pair is called a `cons` cell.

This implementation implies that in order to count the number of elements in the list, or to get to the last element, you have to step through _every_ element in the list, hinting toward the kind of performance guarantees you can expect. 

*Vectors*

```clojure
[1 2 3 4] ; 4 element vector

[fred bob mary] ; 3 element vector

[3 "SomeName" blah] ; 3 element vector, showing that type is not relevant

[[1 2] 3 [4 [5]]] ; 3 element vector (a vector, a number and another vector), showing that nested elements are fine.
```

These look incredibly similar to lists, but their performance guarantees differ. They are implemented, effectively, as arrays. 

You can think of it as a row of spots pre-allocated in memory for values, making the size something you have to keep track of and nth element access in constant time (you can just go to spot 16 and look what's there without walking through every value in between)

Vectors are used a lot throughout the evaluator for short lists, arguments and so on, but doesn't do anything special with it.

*Sets*

```clojure
#{1 2 3 4}

#{:fred :bob :mary}
```

You can think of sets in Clojure as mathematical sets. They will not have any duplicate items and are unordered. If you try add a duplicate element to a set, it simply returns the same set.

There are some useful functions for interacting with sets in the `clojure.set` namespace, for example:

```clojure
(set/difference #{1 2 3} #{3 4}) ; #{1 2}
(set/intersection #{1 2 3} #{3 4}) ; #{3}
(set/union #{1 2 3} #{3 4}) ; #{1 2 3 4}
```

*Maps*

The final kind of structure is the venerable map, called various things in various languages (HashMap, Dictionary, Associative Array) - It needs to be specified as exactly an even number of elements, forming key/value pairs:

```clojure
;; A typical map in clojure.
{:name "Bob"
 :age 34
 :miles 500}

;; An example of a map with different types in both key and value spots
{"Name" :bob
 [1 2] true
 [1 3] {:colour "blue"}}
```

You can have any type in the key or value parts - this is quite useful for stuff like coordinates you want to assign some state to, for example. 

In general, most maps use Clojure keywords in the key part due to it being optimized for lookup and easier to read.

*IMPORTANT!* Maps are _unordered_ - you simply can't order them, and if need ordered keypairs, then you probably want to rethink your needs first.

##### Special forms

*Tagged literals*

```clojure
#inst "2022-02-22 20:22:02" 
#uuid "some-uuid-string" 
```

You'll sometimes see these, don't stress. They usually resolve to a specific type, depending on the hosting platform. It _is_ possible to provide your own tags to Clojure, but I would caution that you want to be very sure you _want_ to do this.

*Functions*

```clojure
#(+ 1 2 %)
```

reads equivalent to 

```clojure
(fn [x] (+ 1 2 x))
```

This is just a bit of sugar for when you need a quick short function value somewhere.

*Other*

There's a bunch of bitsy bits around macro's (code that the reader translates to different code) and other more specific forms, but that's outside the scope for this point.

#### Eval

So now you can reason about the specific data structure the Clojure reader turns text into, we can move onto the Eval step - this is where the real action lies.

Effectively, when we want to evaluate our code, we first need to construct and _environment_ that the code will run in. Sometimes the code can alter the environment or create copies of the environment in order to make smaller scoped environments.

I'll explain more about environments in a bit, but keep that in mind as we go through the process.

Here's some 'psuedocode' steps that the evaluator takes when evaluating any clojure code:

- Given a `form` as the structure given to the evaluator
- Switch against the `form` type:
  - Symbol? Look the symbol value up in the _environment_ and return that
  - Literal type (number, string, etc)? Just return it.
  - Vector/Set/Map? Walk through each element, evaluating and replacing each with the result of that evaluation
  - List? Walk through each element, evaluate it to resolve to a value _then_ call the first element as a function value with the rest of the elements as arguments to the function. (if you see IFn exceptions, it's often due to this not resolving to a function)

You'll notice that this is self-referring (recursive) so that it ends up walking all the datastructures read by the reader.

```clojure
(+ 1 2)
```

With the above form, the reader turns this into a 3 element `list`. Because it is a list, the evaluator will first evaluate each element - `+` resolving to the core `+` function value (exception if it couldn't be found in the _environment_), and `1` an `2` resolving to themselves.

Once all the inner values are evaluated, we then call the `+` function with the rest of the elements as arguments (`1` and `2` in this case) and return the final value (simply `3` in this case)

Some functions, like `defn` or `def` can alter the environment by adding or replacing elements - this is how you can define functions for the evaluator to look for when evaluating.

Other functions, like `let` or `loop` will create mini environments that the evaluator can use in its form scope, but that is discarded outside of their form. This is what allows local scope to happen.

Once you understand that the environment is effectively a map with key/value pairs, you can reason accurately about a large portion of Clojure code - or, really, code in any language - it's just fundamental Computer Science.

#### Print

The evaluator having come up with some final value after running, the printer will take that value and encode the data structure as a Clojure edn string and print it out.

#### Loop

GOTO Read until process is killed.

### That's a wrap.

And that's all there is to it. Believe it or not, now you just need to learn the specific function set of Clojure and what tools you have at your exposure to write code that actually _does_ something.

But you're now able to accurately reason about what the code transforms to and how the code will behave, everything (well, let's ignore macro's for now) has to play by these rules.

For help on the various core functions you can call, look here:

https://clojure.org/api/cheatsheet
https://cljs.info/cheatsheet/

#### Ok, fine, I'll quickly go over some of the most common functions:

*def*: http://clojuredocs.org/clojure.core/def

```clojure
(def x 1)
(def person {:name "Bob"})
```

This just alters the environment, making the symbols `x` and `person` in the above code resolve to the values `1` and `{:name "Bob"}` respectively.

*fn*: http://clojuredocs.org/clojure.core/fn

```clojure
(fn [a b] (+ a b))
```

`fn` returns a function that, when called, evaluates its body and returns the evaluated value. In the above case, we have a contrived `+` that simply adds `a` and `b`. The vector is a list of symbols that gets assigned in this functions' locally scoped environment.

You could run it like (wrap the function in a list):

```clojure
((fn [a b] (+ a b)) 1 2) ;; returns 3
```

This is a contrived example, so there's:

*defn*: http://clojuredocs.org/clojure.core/defn

```clojure
(defn add [a b]
  (+ a b))
```

This is really just shorthand for:

```clojure
(def add (fn [a b] (+ a b)))
```

Not a whole lot more to add here.

Actually, just go through this: https://www.braveclojure.com/do-things/ - Daniel does a much better job here, and it'll do you good to see the data stuff in a different, more pragmatic angle.

Please _don't_ go installing emacs according to the book though, unless you feel like a long zen journey. In which case, heck, why not?
