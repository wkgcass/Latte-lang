# Latte-lang TODOs

## 1. Full Function Support

The current `function type` support is not good enough. Any thing that is invokable should be seen as a function.

Here's the what to do:

* remove inner method, replace with lambda (EASY)
* use `fun` as modifier when defining methods (just like kotlin) (EASY)
* constructor as a function, when calling the function, a new object would be returned (MEDIUM)
* method as a function, when calling the function, the method would be invoked (MEDIUM)
* bind argument to function, which generates a new function (HARD)
