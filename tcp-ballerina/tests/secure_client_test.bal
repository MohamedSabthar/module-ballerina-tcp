import ballerina/test;
import ballerina/jballerina.java;
import ballerina/io;

@test:BeforeSuite
function setupServer() {
    var result = startSecureServer();
}

// @test:Config {dependsOn: [testServerAlreadyClosed], enable: true}
// function testProtocolVersion() returns @tainted error? {
//     io:println("\n\n\n\n\n\n\n\nstart testProtocolVersion");
//     Error|Client socketClient = new ("localhost", 9002, secureSocket = {
//         certificate: {path: certPath},
//         protocol: {
//             name: "TLS",
//             versions: ["TLSv1.1"] // server only support TLSv1.2 but client only support TLSv1.1 write should fail
//         },
//         ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"]
//     });

//     if (socketClient is Client) {
//         test:assertFail(msg = "Server only support TLSv1.2 initialization should fail.");
//         check socketClient->close();
//     }
//     io:println("SecureClient: ", socketClient);
//     io:println("end testProtocolVersion");
// }

// @test:Config {dependsOn: [testProtocolVersion], enable: true}
// function testCiphers() returns @tainted error? {
//     io:println("\n\n\n\n\n\n\n\nstart testCiphers");
//     Error|Client socketClient = new ("localhost", 9002, secureSocket = {
//         certificate: {path: certPath},
//         protocol: {
//             name: "TLS",
//             versions: ["TLSv1.2", "TLSv1.1"]
//         },
//         ciphers: ["TLS_RSA_WITH_AES_128_CBC_SHA"] // server only support TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA write should fail
//     });

//     if (socketClient is Client) {
//         test:assertFail(msg = "Server only support TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA cipher initialization should fail.");
//         check socketClient->close();
//     }
//     io:println("SecureClient: ", socketClient);
//     io:println("end testCiphers");
// }

@test:Config {dependsOn: [testServerAlreadyClosed], enable: true}
function testSecureClientEcho() returns @tainted error? {
    io:println("\n\n\n\n\n\n\n\nstart testSecureClientEcho");
    Client socketClient = check new ("localhost", 9002, secureSocket = {
        certificate: {path: certPath},
        protocol: {
            name: "TLS",
            versions: ["TLSv1.2", "TLSv1.1"]
        },
        ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"]
    });

    string msg = "Hello Ballerina Echo from secure client";
    byte[] msgByteArray = msg.toBytes();
    check socketClient->writeBytes(msgByteArray);

   readonly & byte[] receivedData = check socketClient->readBytes();
   test:assertEquals('string:fromBytes(receivedData), msg, "Found unexpected output");

    check socketClient->close();
    io:println("end testSecureClientEcho");
}

@test:Config {dependsOn: [testSecureClientEcho], enable: true}
function testSecureListenerWithSecureClient() returns @tainted error? {
    io:println("\n\n\n\n\n\n\n\nstart testSecureListenerWithSecureClient");
    Client socketClient = check new ("localhost", 9002, secureSocket = {
        certificate: {path: certPath},
        protocol: {
            name: "TLS",
            versions: ["TLSv1.2", "TLSv1.1"]
        },
        ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"]
    });

    string msg = "Hello Ballerina Echo from secure client";
    byte[] msgByteArray = msg.toBytes();
    check socketClient->writeBytes(msgByteArray);

    readonly & byte[] receivedData = check socketClient->readBytes();
    test:assertEquals('string:fromBytes(receivedData), msg, "Found unexpected output");

    check socketClient->close();
    io:println("end testSecureListenerWithSecureClient");
}

// @test:Config {dependsOn: [testSecureListenerWithSecureClient], enable: true }
// function testSecureListenerWithClient() returns @tainted error? {
//     io:println("\n\n\n\n\n\n\n\nstart testSecureListenerWithClient");
//     Client socketClient = check new ("localhost", 9002);

//     // This is not a secureClient since this is not a handshake msg,
//     // this write will close the connection, so client will get Server already closed error.
//     check socketClient->writeBytes("msg".toBytes());

//     Error|(readonly & byte[]) response = socketClient->readBytes();
//     if (response is readonly & byte[]) {
//         test:assertFail(msg = "Accessing secure server without secure client configuratoin, read should fail.");
//     } else {
//         io:println(response);
//     }

//     check socketClient->close();
//     io:println("end testSecureListenerWithClient");
// }

@test:AfterSuite {}
function stopServer() {
    var result = stopSecureServer();
}

public function startSecureServer() returns Error? = @java:Method {
    name: "startSecureServer",
    'class: "org.ballerinalang.stdlib.tcp.testutils.TestUtils"
} external;

public function stopSecureServer() returns Error? = @java:Method {
    name: "stopSecureServer",
    'class: "org.ballerinalang.stdlib.tcp.testutils.TestUtils"
} external;
