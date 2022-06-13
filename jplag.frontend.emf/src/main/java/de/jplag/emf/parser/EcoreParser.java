package de.jplag.emf.parser;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import de.jplag.AbstractParser;
import de.jplag.ErrorConsumer;
import de.jplag.TokenConstants;
import de.jplag.TokenList;
import de.jplag.emf.Language;
import de.jplag.emf.MetamodelToken;

/**
 * Parser for EMF metamodels.
 * @author Timur Saglam
 */
public class EcoreParser extends AbstractParser {
    protected TokenList tokens;
    protected String currentFile;
    protected MetamodelTreeView treeView;
    protected AbstractMetamodelVisitor visitor;

    /**
     * Creates the parser.
     * @param errorConsumer is the consumer for any occurring errors.
     */
    public EcoreParser(ErrorConsumer errorConsumer) {
        super(errorConsumer);
        EcorePackage.eINSTANCE.eClass();
        final Resource.Factory.Registry registry = Resource.Factory.Registry.INSTANCE;
        final Map<String, Object> extensionMap = registry.getExtensionToFactoryMap();
        extensionMap.put(EcorePackage.eNAME, new XMIResourceFactoryImpl());
    }

    /**
     * Parses all tokens form a list of files.
     * @param directory is the base directory.
     * @param fileNames is the list of file names.
     * @return the list of parsed tokens.
     */
    public TokenList parse(File directory, List<String> fileNames) {
        tokens = new TokenList();
        errors = 0;
        for (String fileName : fileNames) {
            currentFile = fileName;
            treeView = new MetamodelTreeView(currentFile);
            List<EObject> rootElements = loadModel(directory.toString(), fileName);
            for (EObject root : rootElements) {
                visitor = createMetamodelVisitor();
                visitor.visit(root);
            }
            tokens.addToken(new MetamodelToken(TokenConstants.FILE_END, fileName + Language.VIEW_FILE_SUFFIX));
            treeView.writeToFile(directory, Language.VIEW_FILE_SUFFIX);
        }
        return tokens;
    }

    /**
     * Extension point for subclasses to employ different token generators.
     * @return a token generating metamodel visitor.
     */
    protected AbstractMetamodelVisitor createMetamodelVisitor() {
        return new MetamodelTokenGenerator(this);
    }

    private List<EObject> loadModel(String directory, String name) {
        final ResourceSet resourceSet = new ResourceSetImpl();
        String pathName = name.isEmpty() ? directory : directory + File.separator + name;
        Resource resource = resourceSet.getResource(URI.createFileURI(pathName), true);
        return resource.getContents();
    }

    public void addToken(int type, EObject source) {
        MetamodelToken token = new MetamodelToken(type, currentFile + Language.VIEW_FILE_SUFFIX, source);
        treeView.addToken(token, visitor.getCurrentTreeDepth());
        tokens.addToken(token);
    }
}