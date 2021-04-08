/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.tcp.compiler;

import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.stdlib.tcp.Constants;

import java.io.PrintStream;

/**
 * Class to Validate TCP services.
 */
public class TcpServiceValidator {
    private SyntaxNodeAnalysisContext ctx;

    public static final String TCP_103 = "TCP_103";
    public static final String FUNCTION_0_NOT_ACCEPTED_BY_THE_SERVICE = "Function `{0}` not accepted by the service";

    public TcpServiceValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ctx = syntaxNodeAnalysisContext;
    }

    public void validate() {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        serviceDeclarationNode.members().stream()
                .filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION
                        || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).forEach(node -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String functionName = functionDefinitionNode.functionName().toString();
            if (Utils.hasRemoteKeyword(functionDefinitionNode) && !Utils.equals(functionName, Constants.ON_CONNECT)) {
                reportInvalidFunction(functionDefinitionNode);
            } else if (Utils.equals(functionName, Constants.ON_CONNECT)) {

                ReturnStatementNodeVisitor returnStatementNodeVisitor = new ReturnStatementNodeVisitor();
                functionDefinitionNode.accept(returnStatementNodeVisitor);

                // iterate the return statements and handle
                // (1) return new HelloService();
                // (2) service = new HelloService(); return service;
                // (3) return functionThatReturnsAConnectionService();

                for (ReturnStatementNode returnStatementNode : returnStatementNodeVisitor.getReturnStatementNodes()) {
                    ExpressionNode expressionNode = returnStatementNode.expression().get();
                    TypeReferenceTypeSymbol typeReferenceTypeSymbol;

                    //(1) return new HelloService();
                    if (expressionNode instanceof ExplicitNewExpressionNode) {
                        typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) ctx.semanticModel()
                                .symbol(expressionNode).get();
                        String serviceClassName = typeReferenceTypeSymbol.typeDescriptor().getName().get();
                        // problems
                        //1. Node visitor only visits the node in single bal file
                        ConnectionServiceClassVisitor connectionServiceClassVisitor =
                                new ConnectionServiceClassVisitor(serviceClassName);
                        ctx.syntaxTree().rootNode().accept(connectionServiceClassVisitor);
                        ClassDefinitionNode classDefinitionNode =
                                connectionServiceClassVisitor.getClassDefinitionNode();
                        // 2. couldn't find a way to get the prefix if the module imported as alias
                        // (.ie) `import ballerina/tcp as t;` prefix -> t
                        String prefix = getPrefix(ctx); // this is wrong because,
                        // getPrefix() only get the prefix from current file if
                        // ConnectionService class defined in another file it may can have different prefix.
                        if (classDefinitionNode == null) {
                            PrintStream console = System.out;
                            console.println("class not found");
                        } else {
                            TcpConnectionServiceValidator tcpConnectionServiceValidator =
                                    new TcpConnectionServiceValidator(ctx, prefix, classDefinitionNode);
                            tcpConnectionServiceValidator.validate();
                        }
                    }
                    // handle (2,3)
                }
            }
        });
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(TCP_103, FUNCTION_0_NOT_ACCEPTED_BY_THE_SERVICE,
                DiagnosticSeverity.ERROR);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString()));
    }

    private String getPrefix(SyntaxNodeAnalysisContext ctx) {
        ModulePartNode modulePartNode = ctx.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclaration : modulePartNode.imports()) {
            if (Utils.equals(importDeclaration.moduleName().get(0).toString().stripTrailing(), (Constants.TCP))) {
                if (importDeclaration.prefix().isPresent()) {
                    return importDeclaration.prefix().get().children().get(1).toString()
                            + SyntaxKind.COLON_TOKEN.stringValue();
                }
                break;
            }
        }
        return Constants.TCP + SyntaxKind.COLON_TOKEN.stringValue();
    }
}
