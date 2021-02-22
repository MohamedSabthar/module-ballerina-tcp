// import ballerina/test;
// import ballerina/io;

// @test:Config {dependsOn: [testSecureClientEcho], enable: true}
// function testSecureListenerWithSecureClient() returns @tainted error? {
//     io:println("\n\n\n\n\n\n start testSecureListenerWithSecureClient");
//     Client socketClient = check new ("localhost", 9002, secureSocket = {
//         certificate: {path: certPath},
//         protocol: {
//             name: "TLS",
//             versions: ["TLSv1.2", "TLSv1.1"]
//         },
//         ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"]
//     });

//     string msg = "Hello Ballerina Echo from secure client";
//     byte[] msgByteArray = msg.toBytes();
//     check socketClient->writeBytes(msgByteArray);

//     readonly & byte[] receivedData = check socketClient->readBytes();
//     test:assertEquals('string:fromBytes(receivedData), msg, "Found unexpected output");

//     check socketClient->close();
//     io:println("end testSecureListenerWithSecureClient");
// }

// @test:Config {dependsOn: [testSecureListenerWithSecureClient], enable: true}
// function testSecureListenerWithClient() returns @tainted error? {
//     io:println("\n\n\n\n\n\n start testSecureListenerWithClient");
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
