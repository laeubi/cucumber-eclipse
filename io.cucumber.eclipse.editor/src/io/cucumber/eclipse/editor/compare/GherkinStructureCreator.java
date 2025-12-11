package io.cucumber.eclipse.editor.compare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import io.cucumber.eclipse.editor.document.GherkinEditorDocument;
import io.cucumber.messages.types.Background;
import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Rule;
import io.cucumber.messages.types.RuleChild;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.TableRow;

/**
 * Structure creator for Cucumber/Gherkin feature files.
 * <p>
 * This class provides structured compare support for .feature files in the Eclipse Compare framework.
 * It parses Gherkin documents and creates a hierarchical structure of nodes representing Features,
 * Scenarios, Steps, etc., enabling semantic comparison rather than just line-by-line text comparison.
 * </p>
 * 
 * @see IStructureCreator
 */
public class GherkinStructureCreator implements IStructureCreator {

	@Override
	public String getName() {
		return "Gherkin Structure Compare";
	}

	@Override
	public IStructureComparator getStructure(Object input) {
		if (!(input instanceof IStreamContentAccessor)) {
			// Log when input type is unexpected to aid debugging
			System.err.println("GherkinStructureCreator: Unexpected input type: " + 
				(input != null ? input.getClass().getName() : "null"));
			return null;
		}

		IStreamContentAccessor accessor = (IStreamContentAccessor) input;
		try (InputStream stream = accessor.getContents()) {
			String content = readContent(stream, accessor);
			return createStructure(content);
		} catch (CoreException e) {
			System.err.println("GherkinStructureCreator: Error reading content: " + e.getMessage());
			return null;
		} catch (IOException e) {
			System.err.println("GherkinStructureCreator: IO error reading content: " + e.getMessage());
			return null;
		} catch (Exception e) {
			System.err.println("GherkinStructureCreator: Unexpected error: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Read content from stream with proper encoding
	 */
	private String readContent(InputStream stream, IStreamContentAccessor accessor) throws IOException, CoreException {
		String charset = StandardCharsets.UTF_8.name();
		if (accessor instanceof IEncodedStreamContentAccessor) {
			String encoding = ((IEncodedStreamContentAccessor) accessor).getCharset();
			if (encoding != null) {
				charset = encoding;
			}
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset))) {
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}

	/**
	 * Create hierarchical structure from Gherkin content
	 */
	private IStructureComparator createStructure(String content) {
		IDocument document = new Document(content);
		GherkinEditorDocument gherkinDoc = GherkinEditorDocument.get(document);

		// Create root node
		GherkinNode root = new GherkinNode(null, "Feature File", GherkinNode.FEATURE_FILE, content, 0, content.length());

		Optional<Feature> featureOpt = gherkinDoc.getFeature();
		if (!featureOpt.isPresent()) {
			return root;
		}

		Feature feature = featureOpt.get();

		// Create feature node
		int featureStart = getOffset(document, feature.getLocation());
		int featureEnd = content.length();
		String featureName = feature.getName() != null && !feature.getName().isEmpty() ? feature.getName() : "Feature";
		GherkinNode featureNode = new GherkinNode(root, featureName, GherkinNode.FEATURE, 
				content, featureStart, featureEnd - featureStart);
		root.addChild(featureNode);

		// Process feature children (Background, Scenarios, Rules)
		for (FeatureChild child : feature.getChildren()) {
			processFeatureChild(featureNode, child, document, content);
		}

		return root;
	}

	/**
	 * Process a feature child element (Background, Scenario, or Rule)
	 */
	private void processFeatureChild(GherkinNode parent, FeatureChild child, IDocument document, String content) {
		// Background
		Optional<Background> bgOpt = child.getBackground();
		if (bgOpt.isPresent()) {
			Background bg = bgOpt.get();
			int start = getOffset(document, bg.getLocation());
			int end = getEndOffset(document, bg.getSteps(), content);
			String name = bg.getName() != null && !bg.getName().isEmpty() ? bg.getName() : "Background";
			GherkinNode bgNode = new GherkinNode(parent, name, GherkinNode.BACKGROUND, 
					content, start, end - start);
			parent.addChild(bgNode);

			// Add steps
			for (Step step : bg.getSteps()) {
				addStepNode(bgNode, step, document, content);
			}
		}

		// Scenario
		Optional<Scenario> scenarioOpt = child.getScenario();
		if (scenarioOpt.isPresent()) {
			Scenario scenario = scenarioOpt.get();
			int start = getOffset(document, scenario.getLocation());
			int end = getEndOffset(document, scenario, content);
			String name = scenario.getName() != null && !scenario.getName().isEmpty() ? scenario.getName() : "Scenario";
			GherkinNode scenarioNode = new GherkinNode(parent, name, GherkinNode.SCENARIO, 
					content, start, end - start);
			parent.addChild(scenarioNode);

			// Add steps
			for (Step step : scenario.getSteps()) {
				addStepNode(scenarioNode, step, document, content);
			}

			// Add examples if present
			for (Examples examples : scenario.getExamples()) {
				addExamplesNode(scenarioNode, examples, document, content);
			}
		}

		// Rule
		Optional<Rule> ruleOpt = child.getRule();
		if (ruleOpt.isPresent()) {
			Rule rule = ruleOpt.get();
			int start = getOffset(document, rule.getLocation());
			int end = getEndOffsetForRule(document, rule, content);
			String name = rule.getName() != null && !rule.getName().isEmpty() ? rule.getName() : "Rule";
			GherkinNode ruleNode = new GherkinNode(parent, name, GherkinNode.RULE, 
					content, start, end - start);
			parent.addChild(ruleNode);

			// Process rule children
			for (RuleChild ruleChild : rule.getChildren()) {
				processRuleChild(ruleNode, ruleChild, document, content);
			}
		}
	}

	/**
	 * Process a rule child element (Background or Scenario)
	 */
	private void processRuleChild(GherkinNode parent, RuleChild child, IDocument document, String content) {
		// Background
		Optional<Background> bgOpt = child.getBackground();
		if (bgOpt.isPresent()) {
			Background bg = bgOpt.get();
			int start = getOffset(document, bg.getLocation());
			int end = getEndOffset(document, bg.getSteps(), content);
			String name = bg.getName() != null && !bg.getName().isEmpty() ? bg.getName() : "Background";
			GherkinNode bgNode = new GherkinNode(parent, name, GherkinNode.BACKGROUND, 
					content, start, end - start);
			parent.addChild(bgNode);

			// Add steps
			for (Step step : bg.getSteps()) {
				addStepNode(bgNode, step, document, content);
			}
		}

		// Scenario
		Optional<Scenario> scenarioOpt = child.getScenario();
		if (scenarioOpt.isPresent()) {
			Scenario scenario = scenarioOpt.get();
			int start = getOffset(document, scenario.getLocation());
			int end = getEndOffset(document, scenario, content);
			String name = scenario.getName() != null && !scenario.getName().isEmpty() ? scenario.getName() : "Scenario";
			GherkinNode scenarioNode = new GherkinNode(parent, name, GherkinNode.SCENARIO, 
					content, start, end - start);
			parent.addChild(scenarioNode);

			// Add steps
			for (Step step : scenario.getSteps()) {
				addStepNode(scenarioNode, step, document, content);
			}

			// Add examples if present
			for (Examples examples : scenario.getExamples()) {
				addExamplesNode(scenarioNode, examples, document, content);
			}
		}
	}

	/**
	 * Add a step node
	 */
	private void addStepNode(GherkinNode parent, Step step, IDocument document, String content) {
		int start = getOffset(document, step.getLocation());
		int end = getEndOffsetForStep(document, step, content);
		String stepText = step.getKeyword() + step.getText();
		GherkinNode stepNode = new GherkinNode(parent, stepText, GherkinNode.STEP, 
				content, start, end - start);
		parent.addChild(stepNode);
	}

	/**
	 * Add an examples node
	 */
	private void addExamplesNode(GherkinNode parent, Examples examples, IDocument document, String content) {
		int start = getOffset(document, examples.getLocation());
		int end = getEndOffsetForExamples(document, examples, content);
		String name = examples.getName() != null && !examples.getName().isEmpty() ? examples.getName() : "Examples";
		GherkinNode examplesNode = new GherkinNode(parent, name, GherkinNode.EXAMPLES, 
				content, start, end - start);
		parent.addChild(examplesNode);
	}

	/**
	 * Get character offset from location
	 */
	private int getOffset(IDocument document, Location location) {
		try {
			int line = location.getLine().intValue() - 1;
			int column = location.getColumn().orElse(1L).intValue() - 1;
			return document.getLineOffset(line) + column;
		} catch (BadLocationException e) {
			System.err.println("GherkinStructureCreator: Invalid location - line: " + 
				location.getLine() + ", column: " + location.getColumn().orElse(0L));
			return 0;
		}
	}

	/**
	 * Get end offset for a list of steps
	 */
	private int getEndOffset(IDocument document, List<Step> steps, String content) {
		if (steps.isEmpty()) {
			return content.length();
		}
		Step lastStep = steps.get(steps.size() - 1);
		return getEndOffsetForStep(document, lastStep, content);
	}

	/**
	 * Get end offset for a scenario (including examples)
	 */
	private int getEndOffset(IDocument document, Scenario scenario, String content) {
		List<Examples> examplesList = scenario.getExamples();
		if (!examplesList.isEmpty()) {
			Examples lastExample = examplesList.get(examplesList.size() - 1);
			return getEndOffsetForExamples(document, lastExample, content);
		}

		List<Step> steps = scenario.getSteps();
		if (!steps.isEmpty()) {
			return getEndOffset(document, steps, content);
		}

		// Just the scenario line
		try {
			int line = scenario.getLocation().getLine().intValue() - 1;
			int offset = document.getLineOffset(line);
			int length = document.getLineLength(line);
			return offset + length;
		} catch (Exception e) {
			return content.length();
		}
	}

	/**
	 * Get end offset for a step
	 */
	private int getEndOffsetForStep(IDocument document, Step step, String content) {
		try {
			int line = step.getLocation().getLine().intValue() - 1;
			
			// Check if step has a data table or doc string
			if (step.getDataTable().isPresent()) {
				List<TableRow> rows = step.getDataTable().get().getRows();
				if (!rows.isEmpty()) {
					TableRow lastRow = rows.get(rows.size() - 1);
					line = lastRow.getLocation().getLine().intValue() - 1;
				}
			} else if (step.getDocString().isPresent()) {
				Location docStringLoc = step.getDocString().get().getLocation();
				line = docStringLoc.getLine().intValue() - 1;
				// Doc strings span multiple lines, find the closing delimiter
				String delimiter = step.getDocString().get().getDelimiter();
				for (int i = line + 1; i < document.getNumberOfLines(); i++) {
					String lineContent = document.get(document.getLineOffset(i), document.getLineLength(i));
					if (lineContent.trim().equals(delimiter)) {
						line = i;
						break;
					}
				}
			}
			
			int offset = document.getLineOffset(line);
			int length = document.getLineLength(line);
			return offset + length;
		} catch (BadLocationException e) {
			System.err.println("GherkinStructureCreator: Invalid location in step: " + step.getText());
			return content.length();
		}
	}

	/**
	 * Get end offset for examples
	 */
	private int getEndOffsetForExamples(IDocument document, Examples examples, String content) {
		try {
			List<TableRow> tableBody = examples.getTableBody();
			if (!tableBody.isEmpty()) {
				TableRow lastRow = tableBody.get(tableBody.size() - 1);
				int line = lastRow.getLocation().getLine().intValue() - 1;
				int offset = document.getLineOffset(line);
				int length = document.getLineLength(line);
				return offset + length;
			}

			// Just header
			Optional<TableRow> headerOpt = examples.getTableHeader();
			if (headerOpt.isPresent()) {
				int line = headerOpt.get().getLocation().getLine().intValue() - 1;
				int offset = document.getLineOffset(line);
				int length = document.getLineLength(line);
				return offset + length;
			}

			// Just examples keyword line
			int line = examples.getLocation().getLine().intValue() - 1;
			int offset = document.getLineOffset(line);
			int length = document.getLineLength(line);
			return offset + length;
		} catch (BadLocationException e) {
			System.err.println("GherkinStructureCreator: Invalid location in examples");
			return content.length();
		}
	}

	/**
	 * Get end offset for a rule
	 */
	private int getEndOffsetForRule(IDocument document, Rule rule, String content) {
		List<RuleChild> children = rule.getChildren();
		if (children.isEmpty()) {
			try {
				int line = rule.getLocation().getLine().intValue() - 1;
				int offset = document.getLineOffset(line);
				int length = document.getLineLength(line);
				return offset + length;
			} catch (BadLocationException e) {
				System.err.println("GherkinStructureCreator: Invalid location in rule: " + 
					(rule.getName() != null ? rule.getName() : "unnamed"));
				return content.length();
			}
		}

		// Find last child
		RuleChild lastChild = children.get(children.size() - 1);
		if (lastChild.getScenario().isPresent()) {
			return getEndOffset(document, lastChild.getScenario().get(), content);
		} else if (lastChild.getBackground().isPresent()) {
			Background bg = lastChild.getBackground().get();
			return getEndOffset(document, bg.getSteps(), content);
		}

		return content.length();
	}

	@Override
	public IStructureComparator locate(Object path, Object input) {
		// Not implemented - Eclipse will use default behavior
		return null;
	}

	@Override
	public String getContents(Object node, boolean ignoreWhitespace) {
		if (node instanceof GherkinNode) {
			GherkinNode gherkinNode = (GherkinNode) node;
			String content = gherkinNode.getContent();
			if (ignoreWhitespace) {
				return content.trim();
			}
			return content;
		}
		return "";
	}

	@Override
	public void save(IStructureComparator node, Object input) {
		// Not implemented - compare is read-only
	}
}
