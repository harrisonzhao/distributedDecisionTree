alias open="xdg-open"
alias cdesk="cd ~/Desktop"
alias cdown="cd ~/Downloads"
alias ..="cd .."
alias ...="cd ../.."
alias ....="cd ../../.."

export HADOOP_VERSION=2.4.0
export HADOOP_HOME=/afs/ee.cooper.edu/service/hadoop/hadoop-${HADOOP_VERSION}
export HADOOP_MAPRED_HOME=${HADOOP_HOME}
export HADOOP_COMMON_HOME=${HADOOP_HOME}
export HADOOP_HDFS_HOME=${HADOOP_HOME}
export HADOOP_YARN_HOME=${HADOOP_HOME}
export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
export HADOOP_OPTS="-server -d64 -Djava.awt.headless=true -Djava.security.krb5.realm= -Djava.security.krb5.kdc= -Dlog4j.configuration=file:${HADOOP_HOME}/etc/hadoop/log4j.properties"
export HADOOP_HEAPSIZE=2000
export CLASSPATH=.:${HADOOP_CONF_DIR}:$(find "${HADOOP_HOME}" -maxdepth 1 -name '*.jar' |xargs echo  |tr ' ' ':'):$(find "${HADOOP_HOME}/lib" -maxdepth 1 -name '*.jar' |xargs echo  |tr ' ' ':')
export PATH=${HADOOP_HOME}/sbin:${HADOOP_HOME}/bin:${PATH}
alias chadoop="cd ${HADOOP_HOME}"
alias hfs="hadoop fs"

