SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )
java -Xmx2048M -cp $SCRIPTPATH/../classes cs276.assignments.Index Basic $1 $2
