distributedDecisionTree
=======================

A decision tree builder using hadoop 2.3.0 and map reduce. <br>
It produces a binary decision tree formatted in an XML file. <br>
The program assumes that categorical values cannot be parsed as doubles. <br>
treeBuilder contains the project for building the tree. <br>
treeTester contains the project for testing a built tree. <br>

To Run
===========
change directory to either treeBuilder or treeTester <br>
edit the command line arguments through pom.xml <br>
the input files and output class location is specified as command line arguments <br>
to build the jar file: mvn clean compile package <br>
to run the built jar file: mvn exec:exec <br>

for treeBuilder <br>
create /tmp/inputs and /tmp/outputs/ in HDFS <br>
add input files in /tmp/inputs <br>
after running mvn exec:exec and job completes <br>
output tree file will be in /tmp/outputs/tree <br>
output tree file will also be in local project directory <br>
chess dataset obtained from: https://archive.ics.uci.edu/ml/datasets/Chess+(King-Rook+vs.+King-Pawn) <br>
wdbc dataset obtained from: https://archive.ics.uci.edu/ml/datasets/Breast+Cancer+Wisconsin+(Diagnostic) <br>



