JFLAGS = -g
JC = javac -Xlint
.SUFFIXES: .java .class
#.java.class:
#	$(JC) $(JFLAGS) $*.java
#	chmod +x $*.class

#default: classes
default:
	$(JC) @list_javac

classes: $(CLASSES:.java=.class)

clean:
	$(RM) src/*/*.class
