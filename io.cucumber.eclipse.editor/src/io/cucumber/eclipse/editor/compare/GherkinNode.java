package io.cucumber.eclipse.editor.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.swt.graphics.Image;

/**
 * Node representing a structural element in a Gherkin document for compare operations.
 * <p>
 * This class represents nodes in the compare tree view, such as Features, Scenarios, Steps, etc.
 * Each node corresponds to a specific range in the document and has a type that determines
 * its icon and behavior in the compare view.
 * </p>
 */
public class GherkinNode implements IStructureComparator, ITypedElement {

	// Node types
	public static final String FEATURE_FILE = "feature-file";
	public static final String FEATURE = "feature";
	public static final String SCENARIO = "scenario";
	public static final String BACKGROUND = "background";
	public static final String RULE = "rule";
	public static final String STEP = "step";
	public static final String EXAMPLES = "examples";

	private final GherkinNode parent;
	private final String name;
	private final String nodeType;
	private final String content;
	private final int start;
	private final int length;
	private final List<GherkinNode> children = new ArrayList<>();

	/**
	 * Creates a new Gherkin node
	 * 
	 * @param parent   parent node (null for root)
	 * @param name     display name
	 * @param nodeType type of the node
	 * @param content  full document content
	 * @param start    start offset in document
	 * @param length   length of the node's range
	 */
	public GherkinNode(GherkinNode parent, String name, String nodeType, String content, int start, int length) {
		this.parent = parent;
		this.name = name;
		this.nodeType = nodeType;
		// Add bounds checking to prevent StringIndexOutOfBoundsException
		if (start >= content.length()) {
			this.content = "";
		} else {
			int endPos = Math.min(start + length, content.length());
			this.content = content.substring(start, endPos);
		}
		this.start = start;
		this.length = length;
	}

	/**
	 * Add a child node
	 */
	public void addChild(GherkinNode child) {
		children.add(child);
	}

	/**
	 * Get the content of this node
	 */
	public String getContent() {
		return content;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return nodeType;
	}

	@Override
	public Image getImage() {
		// Return null to use default icons
		return null;
	}

	/**
	 * Get the node type
	 */
	public String getNodeType() {
		return nodeType;
	}

	/**
	 * Get the start offset
	 */
	public int getStart() {
		return start;
	}

	/**
	 * Get the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Get the parent node
	 */
	public GherkinNode getParent() {
		return parent;
	}

	@Override
	public Object[] getChildren() {
		return children.toArray();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GherkinNode) {
			GherkinNode other = (GherkinNode) obj;
			return nodeType.equals(other.nodeType) && 
				   name.equals(other.name) &&
				   start == other.start;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeType, name, start);
	}
}
