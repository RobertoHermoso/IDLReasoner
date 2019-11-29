package es.us.isa.idlreasoner.analyzer;


import es.us.isa.idlreasoner.compiler.ResolutorCreator;
import es.us.isa.idlreasoner.mapper.IDLMapper;

import static es.us.isa.idlreasoner.util.PropertyManager.readProperty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Analyzer {

	private ResolutorCreator resolutor;
	private String file = readProperty("aux_constraints");
	private IDLMapper idlMapper;
	private Map<String, Map<String, String>> mappingParameters;
	
	private String idl;
	private String operation;
	private String oasLink;
	private String operationType;
	
	private Map<String, String> restrictions = new HashMap<String, String>();

	public Analyzer(String idl, String oasLink, String operationType, String operation) {
		
		this.initConfigurationFile();
		
		this.resolutor = new ResolutorCreator();
		this.idl = idl;
		this.oasLink = oasLink;
		this.operation = operation;
		this.operationType = operationType;
		
	}
	
	
	public List<Map<String,String>>  getAllRequest() {
		
		this.initDocument();
		this.finishDocumentMinizinc();
		return resolutor.solveGetAllSolutins(this.file);
	}
	
	public Map<String,String> randomRequest() {
		return this.getAllRequest().get(ThreadLocalRandom.current().nextInt(0, resolutor.getMaxResults()));
	
	}

	public Boolean isDeadParameter(String parameter) {
		this.initDocument();
		
		this.idlMapper.setRestriction(parameter, true);

		this.finishDocumentMinizinc();
		Map<String,String> res =  resolutor.solve(this.file);
		
		return res.size()==0;
	}
	


	public Boolean isFalseOptional(String parameter) {
		this.initDocument();

		this.idlMapper.setRestriction(parameter, false);

		this.finishDocumentMinizinc();
		
		Map<String,String> res =  resolutor.solve(this.file);
		
		return res.size()==0;
	}
	
	
	public Boolean isValidIDL() {
		List<String> parameters = this.idlMapper.getParameters();
		Boolean res = true;
		for(String parameter : parameters) {
			res = !this.isDeadParameter(parameter) && !this.isFalseOptional(parameter);
			if(!res) { 
				break;
			}
		}
		return res;
	}
	
	public void setParameter(String parameter, String value) {
		if(this.mappingParameters==null) {
			this.mappingParameters = idlMapper.getMappingParameters();
		}
		restrictions.put(parameter, translateParameter(parameter, value));
	}
	
	
	public void setListParameterToVoid() {
		this.restrictions = new HashMap<String, String>();
	}
	

	public Boolean validRequest() {
		Set<String> parameters = this.restrictions.keySet();
		List<String> allParemeters = this.idlMapper.getParameters();
		this.initDocument();
		for(String p : allParemeters) {
			if(parameters.contains(p)) {
				this.idlMapper.setValue(p, this.restrictions.get(p));
				this.idlMapper.setRestriction(p, true);
			}else {
				this.idlMapper.setRestriction(p, false);

			}
		}
		this.finishDocumentMinizinc();
		return this.resolutor.solve(file).size()!=0;
	}
	
	//TODO
	public Boolean validPartialRequest() {
		Set<String> parameters = this.restrictions.keySet();
		List<String> allParemeters = this.idlMapper.getParameters();
		this.initDocument();
		for(String p : allParemeters) {
			if(parameters.contains(p)) {
				this.idlMapper.setValue(p, this.restrictions.get(p));
				this.idlMapper.setRestriction(p, true);
			}
		}
		this.finishDocumentMinizinc();
		return this.resolutor.solve(file).size()!=0;
	}
	

	public Integer numberOfRequest() {
		return this.getAllRequest().size();
	}
	
	private String translateParameter(String parameter, String value) {
		String res = "1";
		Set<String> parameters = mappingParameters.keySet();
		if(parameters.contains(parameter)) {
			Map<String, String> mapping = mappingParameters.get(parameter);
			res = mapping.get(value);
		}
		
		return res;
	}
	
	private void initDocument() {
		idlMapper = new IDLMapper(idl, operation, oasLink, operationType, resolutor.getDirectory());
	}
	
	private void finishDocumentMinizinc() {
	
		idlMapper.finishMinizincDocument();

	}
	
	private void initConfigurationFile() {
		
		File file = new File("./idl_aux_files/config.properties");
		Boolean exists = file.canRead();

		if(!exists) {
			file.getParentFile().mkdir();
			FileWriter fw;

			try {
				file.createNewFile();
				
				fw = new FileWriter(file);

			    fw.append("compiler: Minizinc\n");
			    fw.append("solver: Chuffed\n");
			    fw.append("fileRoute: " + readProperty("aux_files_folder") + "/" + readProperty("idl_files_folder") + "\n");
			    fw.append("maxResults: 100\n");
			    
			    fw.flush();
			    fw.close();
			} catch (IOException e) {
	
				e.printStackTrace();
			}
		}
	}
	
	
	
	
}
