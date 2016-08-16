#!/bin/bash

set -e
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$JAVA7_HOME
fi
echo ${JAVA_HOME:?}
echo ${ANDROID_NDK:?}

export ANDROID_VERSION="23"
if [ ! -d $ANDROID_NDK/platforms/android-$ANDROID_VERSION ]; then
    export ANDROID_VERSION="21"
fi

NUM_JOBS=4
if [ -f /proc/cpuinfo ]; then
    NUM_JOBS=$(cat /proc/cpuinfo | grep ^processor | wc -l)
fi

if [ `uname` == "Darwin" ]; then
    export SYSTEM="darwin-x86_64"
elif [ `uname -m` = "x86_64" ]; then
    export SYSTEM="linux-x86_64"
else
    export SYSTEM="linux-x86"
fi

SAVED_PATH=$PATH

function build() {
    arch_ver=$1; shift
    arch_suffix=$1
    arch=`echo $arch_ver | sed 's/-.\..$//g'`
    arch_name=`echo $arch | sed s/x86\$/i686-linux-android/ | sed s/x86_64/x86_64-linux-android/`
    arch_short=`echo $arch | cut -d'-' -f 1 | sed s/mipsel/mips/ \
                           | sed s/aarch64/arm64/ | sed s/mips64el/mips64/`
    export PATH=$ANDROID_NDK/toolchains/$arch_ver/prebuilt/$SYSTEM/bin:$SAVED_PATH
    output_dir="../src/main/jniLibs/"`echo $arch_short | sed s/arm$/armeabi/ | sed s/arm64/arm64-v8a/`$arch_suffix
    export CC=${arch_name}-gcc
    export CPP=${arch_name}-cpp
    export CPPFLAGS="--sysroot=$ANDROID_NDK/platforms/android-$ANDROID_VERSION/arch-$arch_short/"
    export CFLAGS="$CPPFLAGS"
    export LDFLAGS=""
    configure_opts="--enable-silent-rules --disable-dependency-tracking --enable-swig-java --enable-endomorphism"

    if [ "$arch" == "arm-linux-androideabi" ]; then
        # --with-asm=arm does not default to enabled under arm, so enable it
        configure_opts="$configure_opts --with-asm=arm"
        if [ -z "$arch_suffix" ]; then
            # armeabi. Only software floating point supported
            export CFLAGS="$CFLAGS -mfloat-abi=soft"
        else
            # armeabi-v7a. Use FPU, but remain compatible with soft-float code.
            export CFLAGS="$CFLAGS -march=armv7-a -mfloat-abi=softfp -mfpu=neon -flax-vector-conversions"
            export LDFLAGS="-march=armv7-a -Wl,--fix-cortex-a8"
        fi
    elif [ "$arch" == "aarch64-linux-android" ]; then
        # arm64-v8a. Hardware floating point and NEON support are built in
        export CFLAGS="$CFLAGS -flax-vector-conversions"
    fi

    echo '============================================================'
    echo $arch$arch_suffix
    echo '============================================================'
    ./configure --host=$arch_name --target=$arch_name $configure_opts >/dev/null
    make -o configure clean -j$NUM_JOBS >/dev/null 2>&1
    make -o configure -j$NUM_JOBS

    mkdir -p $output_dir
    ${arch_name}-strip src/.libs/libwallycore.so
    cp src/.libs/libwallycore.so $output_dir
}

if [ -d libwally-core ]; then
    pushd libwally-core
    need_popd=yes
fi

all_archs=`ls $ANDROID_NDK/toolchains/ | grep -v llvm`

if [ -n "$1" ]; then
    all_archs="$1"
fi

echo '============================================================'
echo Initialising:
echo $all_archs
echo '============================================================'
tools/cleanup.sh
tools/autogen.sh


for a in $all_archs; do
    build $a
    case $a in
        arm-linux-androideabi*)
            build $a -v7a
            ;;
    esac
done

if [ -n "$need_popd" ]; then
    popd
fi
