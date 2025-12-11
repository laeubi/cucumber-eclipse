# Structured Compare Support for Cucumber Feature Files

## Overview

Cucumber Eclipse now provides structured compare support for `.feature` files, enabling semantic comparison rather than simple line-by-line text comparison. This makes it easier to understand differences between feature file versions by organizing changes in a hierarchical structure.

## What is Structured Compare?

Structured compare is an Eclipse Compare framework feature that parses files into a tree structure of logical elements. Instead of showing only text differences, it presents changes in terms of the document's structure (Features, Scenarios, Steps, etc.).

## Features

The structured compare support provides a hierarchical view of Gherkin documents with the following elements:

- **Feature**: Top-level feature container
- **Background**: Background steps shared by scenarios
- **Scenario**: Individual test scenarios
- **Scenario Outline**: Parameterized scenarios with examples
- **Rule**: Business rule containers (Gherkin 6+)
- **Examples**: Data tables for scenario outlines
- **Steps**: Given/When/Then/And/But steps

## How to Use

### Basic Usage

1. Open Eclipse Compare view:
   - Select two `.feature` files in Project Explorer
   - Right-click and choose **Compare With** → **Each Other**
   
2. The Compare view will show:
   - **Structure pane**: Hierarchical tree showing Features, Scenarios, Steps
   - **Content pane**: Side-by-side diff of the selected element

### Comparing with Version Control

1. Right-click a `.feature` file
2. Select **Compare With** → **Previous Revision** (or other version control options)
3. Navigate through the structure tree to see changes at different levels

### Benefits

- **Semantic Understanding**: See what scenarios or steps changed, not just which lines
- **Better Navigation**: Jump directly to changed features or scenarios
- **Clearer Diffs**: Focus on meaningful changes in your test structure
- **Version Control Integration**: Works with Git, SVN, and other version control systems

## Example Structure

Given a feature file:

```gherkin
Feature: Shopping Cart
  
  Background:
    Given the store is open
  
  Scenario: Add item to cart
    When I add "Apple" to cart
    Then cart should contain 1 item
  
  Rule: Discount rules
    
    Scenario: Apply 10% discount
      When total is over $50
      Then apply 10% discount
```

The structure tree will show:
```
├── Shopping Cart (Feature)
│   ├── Background
│   │   └── Given the store is open (Step)
│   ├── Add item to cart (Scenario)
│   │   ├── When I add "Apple" to cart (Step)
│   │   └── Then cart should contain 1 item (Step)
│   └── Discount rules (Rule)
│       └── Apply 10% discount (Scenario)
│           ├── When total is over $50 (Step)
│           └── Then apply 10% discount (Step)
```

## Implementation Details

### Architecture

The implementation consists of:

1. **GherkinStructureCreator**: Implements Eclipse's `IStructureCreator` interface
   - Parses `.feature` files using the Cucumber Gherkin parser
   - Builds a tree of structural nodes
   - Handles encoding and error cases

2. **GherkinNode**: Implements `IStructureComparator` and `ITypedElement`
   - Represents a structural element (Feature, Scenario, Step, etc.)
   - Maintains parent-child relationships
   - Provides content for comparison

3. **Extension Points**: Registered in `plugin.xml`
   - `org.eclipse.compare.structureCreators`: Registers the Gherkin structure creator
   - `org.eclipse.compare.contentViewers`: Provides text merge viewer for content

### Supported Gherkin Elements

- ✅ Feature
- ✅ Background
- ✅ Scenario
- ✅ Scenario Outline
- ✅ Examples
- ✅ Rule (Gherkin 6+)
- ✅ Steps (with data tables and doc strings)
- ✅ Tags (inherited from parent elements)

### Limitations

- Structure comparison is read-only (no merge editing)
- Complex indentation patterns may affect diff visualization
- Comments are part of the text content, not separate nodes

## Technical References

- [Eclipse Compare Framework Documentation](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/compare_structureviewer.htm)
- [Cucumber Gherkin Language](https://cucumber.io/docs/gherkin/reference/)
- [Eclipse Compare Extension Points](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_compare_structureCreators.html)

## Examples

See the example feature files in the `examples/` directory:
- `examples/java-calculator/src/test/resources/io/cucumber/examples/calculator/compare_demo.feature` - Comprehensive example demonstrating all structural elements
- Other example projects show real-world feature file usage

## Future Enhancements

Potential improvements for future versions:

- Custom icons for different node types
- Merge editing support (currently read-only)
- Filtering by tags or keywords
- Collapsible sections in the structure tree
- Integration with test execution history
