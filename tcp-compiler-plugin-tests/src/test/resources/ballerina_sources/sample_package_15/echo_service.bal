import ballerina/tcp as t;

service class EchoService {

     remote function onBytes(readonly & byte[] data, t:Caller caller) returns byte[]|t:Error? {
         check caller->writeBytes(data);
     }

     remote function onClose(int i) returns t:Error? {

     }
 }