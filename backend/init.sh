#!/bin/sh
# Le répertoire root du JDK
#export JAVA_HOME="/home/crios/work/Libs/jdk1.8.0_151"
#export JAVA_HOME="/home/crios/work/Libs/jdk-15.0.2"
export JAVA_HOME="/home/crios/work/Libs/jdk-17.0.10/"
# Config Maven2
export M2_HOME="/home/crios/work/maven"
export MAVEN_OPTS="-Xmx512m"

# Config ANT
# export ANT_HOME=C:\dev\build\ant-1.7.0

#export IDE_PATH=/home/crios/projects/ide/idea-IU-162.2032.8/bin

#export EDITOR_PATH=/opt/sublime_text

# PATH
export MAVEN_HOME="$M2_HOME"
export MAVEN_OPTS="-Xmx4096m"
export PATH=$JAVA_HOME/bin:$M2_HOME/bin:$PATH




