package net.lecousin.neufbox.mediacenter.eclipse;

import net.lecousin.framework.application.Application;

public enum Local {

	Create_new_folder("Create new folder", "Créer un nouveau dossier"),
	Enter_the_name_for_the_new_folder("Enter the name for the new folder", "Entrez le nom du nouveau dossier"),
	Open_Media_Center("Open NeufBox Media Center", "Ouvrir le NeufBox Media Center"),
	Remove("Remove", "Supprimer"),
	selected_items("selected items", "éléments sélectionnés"),
	the_les("the", "les"),
	The_name_cannot_be_empty("The name cannot be empty", "Le nom ne doit pas être vide"),
	This_folder_already_exists("This folder already exists", "Ce dossier existe déjà"),
	
	MSG_remove_confirmation("Are you sure you want to remove %#1% ?", "Etes-vous sûr de vouloir supprimer %#1% ?"),
	;
	
	private Local(String english, String french) {
		this.english = english;
		this.french = french;
	}
	private String english;
	private String french;
	public static String process(Local text, Object...params) {
		int i = 1;
		String str = text.toString();
		for (Object param : params) {
			str = str.replace("%#"+i+"%", param.toString());
			i++;
		}
		return str;
	}
	@Override
	public java.lang.String toString() {
		switch (Application.language) {
		case FRENCH: return french;
		default: return english;
		}
	}
}
