package es.us.isa.idlreasoner.compiler;

import es.us.isa.idlreasoner.util.CommonResources;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static es.us.isa.idlreasoner.util.FileManager.*;
import static es.us.isa.idlreasoner.util.IDLConfiguration.*;
import static es.us.isa.idlreasoner.util.Utils.terminate;

public class Resolutor {

	protected CommonResources cr;
	protected boolean randomSearch;

	public Resolutor(CommonResources cr) {
		this.cr = cr;
	}

	public boolean isRandomSearch() {
		return randomSearch;
	}

	public void setRandomSearch(boolean randomSearch) {
		this.randomSearch = randomSearch;
	}

	public String getSeed() {
		return ThreadLocalRandom.current().nextInt(1, 2146) + Long.toString((new Date().getTime())/1000).substring(4);
	}

	public List<Map<String,String>> solveGetAllSolutions() {
		List<Map<String,String>> res = new ArrayList<Map<String,String>>();
		String results = null;
		while (results == null)
			results =  this.callSolver(getSolveAllCommand(), false);
//		results = fixIfErrors(results, command);
		List<String> resultsSplit = Arrays.asList(results.split(SOLUTION_SEP));

		if (resultsSplit.size() == 2 && resultsSplit.get(0).equals(""))
			res.add(new HashMap<>());
		else if (resultsSplit.size() > 1) {
			for (String r : resultsSplit)
				res.add(this.mapSolutions(r));
			res.remove(res.get(res.size() - 1));
		}
		return res;
	}

	public Map<String,String> solve() {
		String solutions = null;
		while (solutions == null)
			solutions =  this.callSolver(getSolveCommand(), randomSearch);
//		solutions = fixIfErrors(solutions, command);
		return this.mapSolutions(solutions);
	}
	
	//TODO
	public List<String> getExplination() {
		List<String> res = new ArrayList<>();
		res = this.mapExplinationSolution(callSolver(this.getExplinationCommand(), true));
		return res;
	}

	private Map<String,String> mapSolutions(String solutions){
		Map<String, String> res = new HashMap<String, String>();
		List<String> solutionsSplit = Arrays.asList(solutions.split(";"));
		if (solutionsSplit.size()==1) {
			if (solutionsSplit.get(0).contains(SOLUTION_SEP)) // This happens when the op has no params nor deps. The solution is empty but valid
				return res;
			else
				return null; // No solution
		}
		String[] aux;
		for(String sol: solutionsSplit) {
			if(!solutionsSplit.get(solutionsSplit.size() - 1).equals(sol)) {
				aux = sol.split("=");
				res.put(aux[0].trim(), aux[1].trim());
			}
		}
		return res;
	}
	
	private List<String> mapExplinationSolution(String solutions){
		List<String> res = new ArrayList<>();
		
		String splitter = solutions.split("Traces:")[1];
		splitter = splitter.split("%%%mzn-progress")[0];

		List<String> constraints = Arrays.asList(splitter.split("\n"));
		List<String> reasons = new ArrayList<>();
		for (String constraint : constraints) {
			if(!constraint.isEmpty()) {
				List<String> c = Arrays.asList(constraint.split(";"));
				reasons.add(c.get(c.size()-1));
			}
		}
		
		for (String reason : reasons) {
			
			String[] data = Arrays.asList(reason.split(cr.BASE_CONSTRAINTS_FILE)).get(1).split("\\|");
			
			Integer line = new Integer(data[1])-1;
			Integer startCharacter = new Integer(data[2])-1;
			Integer lastCharacter = new Integer(data[4]);
			
			try {
				BufferedReader br = openReader(cr.BASE_CONSTRAINTS_FILE);
				
				List<String> lines = br.lines().collect(Collectors.toList());
				String constraint = lines.get(line).substring(startCharacter, lastCharacter);
				res.add(constraint);
				
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return res;
	}

	private String callSolver(String command, boolean async) {
		String line = "";
		StringBuilder res = new StringBuilder();
		ProcessBuilder processBuilder = new ProcessBuilder();
		Process process = null;
		String[] commandProcessArgs = getCommandProcessArgs();
		processBuilder.command(commandProcessArgs[0], commandProcessArgs[1], command);

		try {
			process = processBuilder.start();
			if (async && !process.waitFor(TIMEOUT, TimeUnit.MILLISECONDS))
				return null;

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = reader.readLine()) != null)
				res.append(line).append("\n");
			reader.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (process != null && process.isAlive()) {
				/*
					The Java API for killing processes is SO-dependent. Also, after calling destroy() or
					destroyForcibly(), the process may still be alive. But ALSO, isAlive() may return false
					(at least in Windows) when the process hasn't been destroyed. Therefore, ignore Java
					and directly kill the child processes by name.
				 */
				killChildProcesses();
			}
		}

		return res.toString();
	}
	
	//TODO
	private String getExplinationCommand(){
		return getMinizincExe() + " --solver findMUS " + cr.BASE_CONSTRAINTS_FILE + " " + cr.DATA_FILE;
	}

	private String getSolveCommand() {
		if (randomSearch)
			return getMinizincExe() + " -r " + getSeed() + " --solver Gecode " + cr.BASE_CONSTRAINTS_FILE + " " + cr.DATA_FILE;
		else
			return getMinizincExe() + " --solver " + SOLVER + " " + cr.BASE_CONSTRAINTS_FILE + " " + cr.DATA_FILE;
	}

	private String getSolveAllCommand() {
		if(!MAX_RESULTS.trim().equals(""))
			return getMinizincExe() + " -n "+ MAX_RESULTS + " --solver " + SOLVER + " " + cr.BASE_CONSTRAINTS_FILE + " " + cr.DATA_FILE;
		else
			return getMinizincExe() + " -a --solver " + SOLVER + " " + cr.BASE_CONSTRAINTS_FILE + " " + cr.DATA_FILE;
	}

	private String getMinizincExe() {
		if (SystemUtils.IS_OS_WINDOWS)
			return "\"minizinc/windows/minizinc.exe\"";
		else if (SystemUtils.IS_OS_MAC)
			return "./minizinc/mac/minizinc";
		else if (SystemUtils.IS_OS_LINUX)
			return "./minizinc/linux/bin/minizinc";
		else
			terminate("Operating system " + System.getProperty("os.name") + " not supported.");

		return null;
	}

	private static void killChildProcesses() {
		ProcessBuilder processBuilder = new ProcessBuilder();
		Process process = null;
		String[] commandProcessArgs = getCommandProcessArgs();
		String[] killCommands = getKillCommands();
		for (String killCommand: killCommands) {
			processBuilder.command(commandProcessArgs[0], commandProcessArgs[1], killCommand);
			try {
				process = processBuilder.start();
				if (process.waitFor() < 0)
					System.err.println("WARNING! Some processes called 'minizinc' or 'fzn-gecode' could not be destroyed. Make sure to destroy them manually.");
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			} finally {
				if (process != null && process.isAlive()) {
					process.destroy();
					if (process.isAlive())
						process.destroyForcibly();
				}
			}
		}
	}

	private static String[] getKillCommands() {
		String[] killCommands = new String[2];
		if (SystemUtils.IS_OS_WINDOWS) {
			killCommands[0] = "taskkill /IM \"fzn-gecode.exe\" /F";
			killCommands[1] = "taskkill /IM \"minizinc.exe\" /F";
		}
		else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			killCommands[0] = "killall -9 fzn-gecode";
			killCommands[1] = "killall -9 minizinc";
		}
		else
			terminate("Operating system " + System.getProperty("os.name") + " not supported.");

		return killCommands;
	}

	private static String[] getCommandProcessArgs() {
		String[] commandProcessArgs = new String[2];
		if (SystemUtils.IS_OS_WINDOWS) {
			commandProcessArgs[0] = "cmd.exe";
			commandProcessArgs[1] = "/c";
		}
		else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			commandProcessArgs[0] = "/bin/bash";
			commandProcessArgs[1] = "-c";
		}
		else
			terminate("Operating system " + System.getProperty("os.name") + " not supported.");

		return commandProcessArgs;
	}

//	private String fixIfErrors(String solutions, String command) {
//		if (SOLVER.toLowerCase().equals("chuffed") && solutions.contains("=====ERROR=====")) {
//			String newCommand = command.replace(SOLVER, "Gecode");
//			return this.callSolver(newCommand);
//		}
//
//		return solutions;
//	}
}
