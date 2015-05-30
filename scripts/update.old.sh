#!/bin/bash

UPDATE_DIR=$(realpath ../instance/updater/)
LIB_DIR=$(realpath ../ConvenienceShare/lib/)
SRCS_DIR=$(realpath ../ConvenienceShare/bin/)


CLASSPATH="."
for file in $(find $LIB_DIR -maxdepth 1 -name *.jar)
do
    CLASSPATH="$CLASSPATH:$file"
done

#rm -rf tmp

if [[ ! -e tmp/jars/srcs ]]
then
    mkdir -pv tmp/jars/srcs
fi

if [[ ! -e tmp/jars/convenience/ ]]
then
    mkdir -pv tmp/jars/convenience/
fi

if [[ ! -e tmp/jars/convenience/res ]]
then
    mkdir -pv tmp/jars/convenience/res
fi

rsync -trv --delete ../ConvenienceShare/src/ ../Common/src/ tmp/jars/srcs
rsync -trv ../ConvenienceShare/src/res/ tmp/jars/convenience/res

DEST=$(realpath tmp/jars/convenience/)

pushd tmp/jars/srcs
for file in $(find  -name *.java | grep -v MainTest.java) #Should move MainTest to test directory
do
    dfile="$DEST$(echo $file | sed 's/.java/.class/' | sed 's/^.//' )"
    if [[ -e $dfile ]]
    then
        destT=$(stat -c "%X" $dfile)
        srcT=$( stat -c "%X" $file)
        
        echo Dest time $destT
        echo Src  time $srcT

        if [[ $destT -ge $srcT ]]
        then
            echo "Already compiled: $file"
            continue;
        fi
    fi
  
    echo "Compiling $file"
    
    javac -cp $CLASSPATH $file -d $DEST || exit
done
popd