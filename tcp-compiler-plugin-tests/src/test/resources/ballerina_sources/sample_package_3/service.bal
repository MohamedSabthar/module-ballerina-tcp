import ballerina/tcp;

service on new tcp:Listener(3000) {

    remote function onConnect(tcp:Caller caller) returns tcp:ConnectionService {
        return new EchoService();
    }
}

service class EchoService {
    *tcp:ConnectionService;

    function onBytes(readonly & byte[] data) {

    }

    function onError(tcp:Error err) returns tcp:Error? {

    }

    function onClose() returns tcp:Error? {

    }
}
