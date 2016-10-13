# RetrofitRxErrorHandler

----
### What is it?

RetrofitRxErrorHandler is an attempt to harden Retrofit HTTP API layer against random errors, like timeouts, 
500 server errors caused due to server overload or accidental loss of networks (happening especially in mobile applications).


### Who should use it?

Everyone who is using [Retrofit](https://square.github.io/retrofit/) combined with 
[RxJava](https://github.com/ReactiveX/RxJava) and experienced random errors when sending requests.


----
## **Usage**
1. Import gradle dependency:

    * Add following lines to you project's main `build.gradle`:
    
        buildscript {
            repositories {
                jcenter()
            }
        }
 
    * Add a dependency to application `build.gradle`:
     
        compile 'com.rzagorski.retrofitrxerrorhandler:retrofitrxerrorhandler:1.0.0'
 
2. Build the strategy:

         RxCallAdapter rxCallAdapter = new RxCallAdapter.Builder()
                     .addBackoffStrategy(Exponential.init()
                             .addThrowable(HttpException.class)
                             .setMaxRetries(3).build())
                     .build();

3. Add it as Retrofit `CallAdapter.Factory`:

         Retrofit retrofit = new Retrofit.Builder()
             .addCallAdapterFactory(new RxErrorHandingFactory(rxCallAdapter))
             .build()

----
### Options:

* different backoff strategies (`Simple`, `Exponential`)
* reactions to different [`Throwables`](http://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html) to strategy
* reactions to [HTTP error codes](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes) to strategy
* exclusive or inclusive behaviour to `Throwables` or HTTP error codes
* maximum retry count (different for every added strategy)

## Examples

[Look into tests](library/src/test/main/java/com/rzagorski/retrofitrxerrorhandler)

## License

    Copyright 2016 Robert Zagórski.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.