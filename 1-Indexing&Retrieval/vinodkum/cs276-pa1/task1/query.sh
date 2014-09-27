SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )
java -Xmx1024M -cp $SCRIPTPATH/../classes cs276.assignments.Query Basic $1
