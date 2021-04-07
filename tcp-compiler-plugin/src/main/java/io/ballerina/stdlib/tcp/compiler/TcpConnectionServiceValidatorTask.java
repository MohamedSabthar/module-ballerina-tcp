/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.tcp.compiler;

import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinalang.stdlib.tcp.Constants;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to validate TCP ConnectionService.
 */
public class TcpConnectionServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        String modulePrefix = getPrefix(ctx);

        ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) ctx.node();
        List<Node> typeReferenceNodes = classDefinitionNode.members()
                .stream().filter(member -> member.kind() == SyntaxKind.TYPE_REFERENCE).collect(Collectors.toList());

        for (Node node : typeReferenceNodes) {
            if (Utils.equals(((TypeReferenceNode) node).typeName().toSourceCode(),
                    modulePrefix + SyntaxKind.COLON_TOKEN.stringValue() + Constants.CONNECTION_SERVICE)) {
                TcpConnectionServiceValidator serviceValidator = new TcpConnectionServiceValidator(ctx,
                        modulePrefix + SyntaxKind.COLON_TOKEN.stringValue());
                serviceValidator.validate();
                return;
            }
        }
    }

    private String getPrefix(SyntaxNodeAnalysisContext ctx) {
        ModulePartNode modulePartNode = ctx.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclaration : modulePartNode.imports()) {
            if (Utils.equals(importDeclaration.moduleName().get(0).toString().stripTrailing(), (Constants.TCP))) {
                if (importDeclaration.prefix().isPresent()) {
                    return importDeclaration.prefix().get().children().get(1).toString();
                }
                break;
            }
        }
        return Constants.TCP;
    }
}
