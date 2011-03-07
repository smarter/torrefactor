JFLAGS = -g
JC = javac -Xlint
.SUFFIXES: .java .class
.PHONY : default classes clean test
#.java.class:
#	$(JC) $(JFLAGS) $*.java
#	chmod +x $*.class

#default: classes
default:
	$(JC) @list_javac

qtgui:
	$(JC) @list_qtgui

all: default qtgui

classes: $(CLASSES:.java=.class)

clean:
	$(RM) src/*/*.class
	$(RM) src/*/*/*.class
	$(RM) test/*/*.class

test: default
	$(JC) @list_test_javac

runtest: test
	java test.core.PieceManagerTest
	java test.util.BencodeTest
	java test.util.RsaTest
