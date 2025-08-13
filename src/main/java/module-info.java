module com.del.testmechine {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;
    requires java.desktop;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    opens com.del.testmechine to javafx.fxml;
    exports com.del.testmechine;
}