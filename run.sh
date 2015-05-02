#!/bin/bash

ps aux | grep java | grep org.json | grep sqlite-jdbc | grep org.cnv.shr.dmn | awk ' { print $2 } ' | sed 's/^/kill /g' | bash

export rootDir=/work/ballin-meme-share

pushd $rootDir/ConvenienceShare/bin/

java -cp .:../libs/org.json-20120521.jar:../libs/sqlite-jdbc-3.8.7.jar: org.cnv.shr.dmn.Main ../../runDir/settings1.props &
java -cp .:../libs/org.json-20120521.jar:../libs/sqlite-jdbc-3.8.7.jar: org.cnv.shr.dmn.Main ../../runDir/settings2.props &

popd
