# Build Blockstream Green

## Build requirements

You need to have the following Android developer tools installed:

- "Android Command-line tools" version 9477386 recommended
- "Android SDK Build-tools" version 33.0.1
- "Android SDK Platform Tools" version 33.0.3 recommended

The above tools can be installed from the Android SDK manager.

## Clone the repo

``` 
git clone https://github.com/Blockstream/green_android.git 
cd green_android 
```

## How to build

#### Use the released native libraries (recommended):

The pre-built native libraries are the same versions used in the builds    
published by Blockstream. Gradle/Android Studio will automatically use the latest.

To download and update manually native libraries to the latest supported run:

``` ./gdk/fetch_android_binaries.sh ```

#### Cross-compile the native libraries (advanced):

If you wish to make changes to the native libraries or would like to build    
completely from source, you can compile [GDK](https://github.com/Blockstream/gdk) from its source code yourself.

This requires the "Android NDK" (version r23b recommended) from Android    
developer tools, as well as [SWIG](http://www.swig.org/). Most Linux    
distributions have SWIG available, on debian for example you can install it
using:

`sudo apt-get install swig`

You must set the environment variables `ANDROID_NDK` and `JAVA_HOME` correctly, then run:
```  
cd crypto  
./prepare_gdk_clang.sh  
cd ..  
```  

If you get errors building please ensure your are using the recommended NDK
version.

#### Build the Android app

Run:

`./gradlew build`

This will build both release and debug builds.

You can speed up builds by limiting the tasks which run. Use `./gradlew --tasks` to see a list of available tasks.

#### Building with docker (optional)

If you have docker configured and want to build the app in release mode
without having to set up an Android development environment, run:

``` 
cd contrib 
docker build -t green_android_builder . 
cd ..  
docker run -it -v $PWD:/ga green_android_builder
```  

If you don't need to build the Docker image, you can instead run:

`docker pull greenaddress/android && docker run -it -v $PWD:/ga greenaddress/android`

Check for various settings of the build script by running it with `--help`  
eg.   
`docker run -it -v $PWD:/ga greenaddress/android --help`

#### APKs
Artifacts are generated in `green/build/outputs/apk/[production]/[release]/`
