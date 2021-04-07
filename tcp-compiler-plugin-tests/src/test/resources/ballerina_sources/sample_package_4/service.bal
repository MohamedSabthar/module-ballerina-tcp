import ballerina/tcp;

service on new tcp:Listener(3000) {

    remote function onConnect(tcp:Caller caller) returns tcp:ConnectionService {
        return new EchoService();
    }
}

service class EchoService {
    *tcp:ConnectionService;

    remote function onBytes() {

    }

    remote function onError() returns tcp:Error? {

    }

    remote function onClose() returns tcp:Error? {

    }
}
