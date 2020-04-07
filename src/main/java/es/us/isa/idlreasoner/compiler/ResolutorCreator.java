package es.us.isa.idlreasoner.compiler;

import java.util.List;
import java.util.Map;

import static es.us.isa.idlreasoner.util.IDLConfiguration.*;
import static es.us.isa.idlreasoner.util.Utils.terminate;

public class ResolutorCreator {

	public static IResolutor createResolutor() {
		IResolutor resolutor = null;

		if (System.getProperty("os.name").contains("Windows"))
			resolutor = new WindowsResolutor();
		else
			terminate("Operating system " + System.getProperty("os.name") + " not supported.");

		return resolutor;
	}

}
