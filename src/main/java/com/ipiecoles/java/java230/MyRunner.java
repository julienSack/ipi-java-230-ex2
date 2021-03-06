package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;


    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     *
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) {
        Stream<String> stream;
        logger.info("Lecture du fichier : " + fileName);

        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        } catch (IOException e) {
            logger.error("Problème dans l'ouverture du fichier " + fileName);
            return new ArrayList<>();
        }

        List<String> lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size() + " lignes lues");
        for (int i = 0; i < lignes.size(); i++) {
            try {
                processLine(lignes.get(i));
            } catch (BatchException e) {
                logger.error("Ligne " + (i + 1) + " : " + e.getMessage() + " => " + lignes.get(i));
            }

        }
        employeRepository.save(employes);
        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     *
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {

        switch (ligne.substring(0, 1)) {
            case "M":
                processManager(ligne);
                break;
            case "T":
                processTechnicien(ligne);
                break;
            case "C":
                processCommercial(ligne);
                break;
            default:
                throw new BatchException(" Type d'employé inconnu ");

        }

    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     *
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        //TODO
        String[] managerFields = ligneManager.split(",");
        Manager m = new Manager();

//controle le matricule, le nombre de champs de la ligne, la date, et le salaire
        checkChampsCommun(managerFields, REGEX_MATRICULE_MANAGER, NB_CHAMPS_MANAGER, "manager");
// appel la fonction pour set les attributs communs
        setEmployes(m, managerFields);
// ajoute a m les differents set
        employes.add(m);
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     *
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        String[] commercialFields = ligneCommercial.split(",");
        Commercial c = new Commercial();

//controle le matricule, le nombre de champs de la ligne, la date, et le salaire
        checkChampsCommun(commercialFields, REGEX_MATRICULE, NB_CHAMPS_COMMERCIAL, "commercial");

// controle le chiffre d'affaire
        try {
            Double.parseDouble(commercialFields[5]);
        } catch (Exception e) {
            throw new BatchException(commercialFields[5] + " n'est pas un nombre valide pour un Chiffre d'affaire");
        }

//controle le coefficient du commercial
        try {
            int performance = Integer.parseInt(commercialFields[6]);
            if (performance < 0 || performance > 100) {
                throw new BatchException("La performance du commercial est incorrecte : " + commercialFields[6]);
            }
        } catch (Exception e) {
            throw new BatchException("La performance du commercial est incorrecte : " + commercialFields[6]);
        }
// appel la fonction pour set les attributs communs
        setEmployes(c, commercialFields);
//set les deux particuliers au commercial
        c.setPerformance(Integer.parseInt(commercialFields[6]));
        c.setCaAnnuel(Double.parseDouble(commercialFields[5]));
// ajoute a c les differents set
        employes.add(c);
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     *
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        //TODO
        String[] technicienFields = ligneTechnicien.split(",");
        Technicien t = new Technicien();

// verifie et set le grade du technicien
        try {
            if (Integer.parseInt(technicienFields[5]) <= 0 || Integer.parseInt(technicienFields[5]) > 5) {
                throw new BatchException("Le grade doit être compris entre 1 et 5 : " + technicienFields[5]);
            } else {
                t.setGrade(Integer.parseInt(technicienFields[5]));
            }
        } catch (Exception e) {
            throw new BatchException("Le grade du technicien est incorrect " + technicienFields[5]);
        }
//controle le matricule, le nombre de champs de la ligne, la date, et le salaire
        checkChampsCommun(technicienFields, REGEX_MATRICULE, NB_CHAMPS_TECHNICIEN, "technicien");

// verifie le matricule du manager du technicien
        if (!technicienFields[6].matches(REGEX_MATRICULE_MANAGER)) {
            throw new BatchException("la chaîne : " + technicienFields[6] + " ne respecte pas l'expression régulière : " + REGEX_MATRICULE_MANAGER);
        }

//set le matricule du manager si il est correct
        employes.stream().forEach(emp -> {
            if (emp instanceof Manager && emp.getMatricule() == technicienFields[6]) {
                t.setManager((Manager) emp);
            }
        });
// verifie si le matricule existe en bdd
        if (employeRepository.findByMatricule(technicienFields[6]) == null && t.getManager() == null) {
            throw new BatchException("Le manager de matricule " + technicienFields[6] + " n'a pas été trouvé dans le fichier ou en base de données");
        }

// appel la fonction pour set les attributs communs
        setEmployes(t, technicienFields);
// ajoute a t les differents set
        employes.add(t);
    }

    /**
     * function commune pour verifier les champs communs des employes
     *
     * @param le matricule, le nombre de champs de la ligne, la date, et le salaire
     */
    private void checkChampsCommun(String[] fields, String RegexMatricule, Integer RegExnbChamps, String typeEmploye) throws BatchException {

//controle le matricule
        if (!fields[0].matches(RegexMatricule)) {
            throw new BatchException("la chaîne : " + fields[0] + " ne respecte pas l'expression régulière : " + RegexMatricule);
        }

//controle le nombre de champs
        if (fields.length != RegExnbChamps) {
            throw new BatchException(" La ligne  " + typeEmploye + " ne contient pas " + RegExnbChamps + " éléments mais " + fields.length);
        }
//controle la date
        try {
            DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(fields[3]);
        } catch (Exception e) {
            throw new BatchException(fields[3] + " ne respecte pas le format de date dd/MM/yyyy");
        }
//controle le salaire
        try {
            Double.parseDouble(fields[4]);
        } catch (Exception e) {
            throw new BatchException(fields[4] + " n'est pas un nombre valide pour un salaire");
        }
    }

    /**
     * fonction pour set les attributs communs des differents employés
     *
     * @param le type d'employé, et le fields de l'employé
     */
    private void setEmployes(Employe e, String[] fields) {

        e.setMatricule(fields[0]);
        e.setNom(fields[1]);
        e.setPrenom(fields[2]);
        e.setDateEmbauche(DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(fields[3]));
        e.setSalaire(Double.parseDouble(fields[4]));

    }
}


