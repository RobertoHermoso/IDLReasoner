
package es.us.isa.idlreasoner.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "parenthesis",
    "operation",
    "parameter"
})
@Generated("jsonschema2pojo")
public class Parenthesis {

    @JsonProperty("parenthesis")
    private List<Parenthesis> parenthesis;
    @JsonProperty("operation")
    private String operation;
    @JsonProperty("parameter")
    private String parameter;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("parenthesis")
    public List<Parenthesis> getParenthesis() {
        return parenthesis;
    }

    @JsonProperty("parenthesis")
    public void setParenthesis(List<Parenthesis> parenthesis) {
        this.parenthesis = parenthesis;
    }

    @JsonProperty("operation")
    public String getOperation() {
        return operation;
    }

    @JsonProperty("operation")
    public void setOperation(String operation) {
        this.operation = operation;
    }

    @JsonProperty("parameter")
    public String getParameter() {
        return parameter;
    }

    @JsonProperty("parameter")
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
