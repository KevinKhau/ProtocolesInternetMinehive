# Depuis la racine, sachant que Minehive est  :
mkdir bin
javac -d bin Minehive/*/*.java
cp -r Minehive/res bin
jar -cfe bin/Server.jar bin/network/Server bin/*/*.class bin/res/*
jar -cfe bin/Host.jar bin/network/Host bin/*/*.class bin/res/*
jar -cfe bin/Client.jar bin/gui/ClientApp bin/*/*.class bin/res/*
java -jar bin/Server.jar
java -jar bin/Client.jar
