THIS_HOME=`pwd`
PID_FILE=${THIS_HOME}/multiple.thread.search.pid

#Checking if pid file exist
if test -e ${PID_FILE}; then
    #if exists, kill process
    PID=$(cat ${PID_FILE})
    kill -s 9 ${PID}
    #delete pid file
    rm -f ${PID_FILE}
else
    #otherwise, exit
    echo "[" ${PID_FILE} "] not exist."
    exit 1
fi
echo "Process "${PID}" is terminated"