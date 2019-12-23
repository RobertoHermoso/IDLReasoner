package es.us.isa.idlreasoner.mapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;

import java.io.*;
import java.util.*;

import static es.us.isa.idlreasoner.util.FileManager.*;
import static es.us.isa.idlreasoner.util.IDLConfiguration.*;

public class OAS2MiniZincVariableMapper extends AbstractVariableMapper {

    private OpenAPI openAPISpec;
    private List<Parameter> parameters;

    public OAS2MiniZincVariableMapper(String apiSpecificationPath, String operationPath, String operationType, MapperResources mr) {
        super(mr);
        this.specificationPath = apiSpecificationPath;

        openAPISpec = new OpenAPIV3Parser().read(apiSpecificationPath);
        if(operationType.equals("get"))
            parameters = openAPISpec.getPaths().get(operationPath).getGet().getParameters();
        if(operationType.equals("delete"))
            parameters = openAPISpec.getPaths().get(operationPath).getDelete().getParameters();
        if(operationType.equals("post"))
            parameters = openAPISpec.getPaths().get(operationPath).getPost().getParameters();
        if(operationType.equals("put"))
            parameters = openAPISpec.getPaths().get(operationPath).getPut().getParameters();
        if(operationType.equals("patch"))
            parameters = openAPISpec.getPaths().get(operationPath).getPatch().getParameters();
        if(operationType.equals("head"))
            parameters = openAPISpec.getPaths().get(operationPath).getHead().getParameters();
        if(operationType.equals("options"))
            parameters = openAPISpec.getPaths().get(operationPath).getOptions().getParameters();

        try {
            mapVariables();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mapVariables() throws IOException {
        if (parameters == null || parameters.size() == 0)
            return;
        mr.operationParameters.clear();
        List<String> previousContent = savePreviousBaseConstraintsFileContent();
        recreateFile(BASE_CONSTRAINTS_FILE);
        initializeStringIntMapping();
        initializeParameterNamesMapping();

        BufferedWriter out = openWriter(BASE_CONSTRAINTS_FILE);
        BufferedWriter requiredVarsOut = openWriter(BASE_CONSTRAINTS_FILE);
        String var;
        String varSet;
        Integer intMapping;

        for (Parameter parameter : parameters) {
            Schema<?> schema = parameter.getSchema();

            if(schema.getType().equals("boolean")) {
                var = "var bool: ";
            } else if(schema.getEnum() != null) {
                if (schema.getType().equals("string")) {
//                    var = "var 0.." + (schema.getEnum().size()-1) + ": ";
                    var = "var {";
                    for (Object o : schema.getEnum()) {
                        intMapping = mr.stringIntMapping.get(o.toString());
                        if (intMapping != null) {
                            var += intMapping + ", ";
                        } else {
                            mr.stringIntMapping.put(o.toString(), mr.stringToIntCounter);
                            var += mr.stringToIntCounter++ + ", ";
                        }
                    }
                    var = var.substring(0, var.length()-2); // trim last comma and space
                    var += "}: ";
                } else if (schema.getType().equals("integer")) {
                    var = "var {";
                    for (Object o : schema.getEnum()) {
                        var += o + ", ";
                    }
                    var = var.substring(0, var.length()-2); // trim last comma and space
                    var += "}: ";
                } else {
                    // TODO: Manage mapping of float enum
                    var = "var float: ";
                }
            } else if(schema.getType().equals("string")) {
                var = "var 0..10000: "; // If string, add enough possible values (10000)
            } else if(schema.getType().equals("integer")) {
                var = "var int: ";
            } else {
                // TODO: Manage mapping of float
                var = "var float: ";
            }
            var += origToChangedParamName(parameter.getName())+";\n";
            out.append(var);

            varSet = "var 0..1: " + origToChangedParamName(parameter.getName())+"Set;\n";
            out.append(varSet);

            if (parameter.getRequired() != null && parameter.getRequired()) {
                mapRequiredVar(requiredVarsOut, parameter);
            }
            mr.operationParameters.put(parameter.getName(), new AbstractMap.SimpleEntry<>(schema.getType(), parameter.getRequired()!=null ? parameter.getRequired() : false));
        }

        out.newLine();
        for (String previousContentLine : previousContent) {
            out.append(previousContentLine + "\n");
        }

        out.flush();
        requiredVarsOut.flush();
        out.close();
        requiredVarsOut.close();

        exportStringIntMappingToFile();
        exportParameterNamesMappingToFile();
    }


    private void mapRequiredVar(BufferedWriter requiredVarsOut, Parameter parameter) throws IOException {
        requiredVarsOut.append("constraint " + origToChangedParamName(parameter.getName())+"Set = 1;\n");
    }

}
