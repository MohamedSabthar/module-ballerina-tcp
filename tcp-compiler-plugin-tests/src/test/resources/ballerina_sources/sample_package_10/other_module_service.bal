import sample_10.module;
import ballerina/tcp;

service on new module:Listener() {
    remote function onConnect() returns module:ConnectionService {
        return new HelloService();
    }
}

service class HelloService {
    *tcp:ConnectionService;
}