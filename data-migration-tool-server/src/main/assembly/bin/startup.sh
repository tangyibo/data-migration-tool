#!/usr/bin/env bash
#############################################
# !!!!!! Modify here please

DS_PROG="data-migration-tool-server-0.0.1.jar"
DS_MAIN="com.weishao.migration.tool.server.ServerApplication"

#############################################

DS_HOME="${BASH_SOURCE-$0}"
DS_HOME="$(dirname "${DS_HOME}")"
DS_HOME="$(cd "${DS_HOME}"; pwd)"
DS_HOME="$(cd "$(dirname ${DS_HOME})"; pwd)"
#echo "Base Directory:${DS_HOME}"

DS_BIN_PATH=$DS_HOME/bin
DS_LIB_PATH=$DS_HOME/lib
DS_CONF_PATH=$DS_HOME/conf

DS_PID_FILE="${DS_HOME}/run/${DS_MAIN}.pid"
DS_RUN_LOG="${DS_HOME}/run/run_${DS_MAIN}.log"

[ -d "${DS_HOME}/run" ] || mkdir -p "${DS_HOME}/run"
cd ${DS_HOME}

echo -n `date +'%Y-%m-%d %H:%M:%S'`             >>${DS_RUN_LOG}
echo "---- Start service [${DS_MAIN}] process. ">>${DS_RUN_LOG}

# JVMFLAGS JVM参数可以在这里设置
JVMFLAGS=-Dfile.encoding=UTF-8

if [ "$JAVA_HOME" != "" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

#把lib下的所有jar都加入到classpath中
CLASSPATH=$DS_CONF_PATH
for i in $DS_LIB_PATH/*.jar
do
	CLASSPATH="$i:$CLASSPATH"
done

nohup $JAVA -cp $CLASSPATH $JVMFLAGS $DS_MAIN $DS_CONF_PATH >>${DS_RUN_LOG} 2>&1 &
#echo "$JAVA -cp $CLASSPATH $JVMFLAGS $DS_MAIN $DS_CONF_PATH >>${DS_RUN_LOG} 2>&1 &"

RETVAL=$?
PID=$!

echo ${PID} >${DS_PID_FILE}
exit ${RETVAL}
