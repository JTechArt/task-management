# Visual Mockup Generation Feature

## Overview
AI-powered visual mockup and HTML prototype generation service for creating interactive UI previews and design mockups using templates and dummy data.

## What This Feature Does
- Generate HTML prototypes from feature specifications
- Create visual mockups with realistic dummy data
- Use Handlebars templates for customization
- Render mockups to PNG using Puppeteer
- Store mockup metadata in database
- Support multiple mockup layouts and themes

## Acceptance Criteria

✅ **Mockup Generation**
- Generate from feature specifications
- Apply templates and themes
- Include realistic dummy data (using Faker.js)
- Export as HTML, PNG, or both

✅ **Template System**
- Handlebars-based templates
- Custom layout support
- Theme variations (modern, classic, minimal)

✅ **Data Generation**
- Use @faker-js/faker for realistic data
- Configurable record counts
- Context-aware data generation

## Technology Stack

**Services**:
- `VisualMockupService.ts` - Mockup generation
- `HTMLPrototypeService.ts` - Interactive prototypes
- `DynamicRuleService.ts` - Rule-based generation

**Libraries**:
```typescript
import * as Handlebars from 'handlebars';
import puppeteer from 'puppeteer';
import { faker } from '@faker-js/faker';
```

**Database Schema**:
```sql
CREATE TABLE visual_mockups (
    id TEXT PRIMARY KEY,
    taskId TEXT,
    featureName TEXT NOT NULL,
    outputPath TEXT NOT NULL,
    format TEXT NOT NULL,
    status TEXT NOT NULL,
    createdAt INTEGER NOT NULL
);
```

## Key Methods

```typescript
class VisualMockupService {
  async generateMockup(request: MockupRequest): Promise<MockupResult>
  async renderTemplate(template: string, data: any): Promise<string>
  async generateDummyData(spec: DummyDataSpec): Promise<any>
  async captureScreenshot(html: string): Promise<Buffer>
}
```

## Implementation Flow

```
1. Receive mockup request
2. Load template from templates/mockups/
3. Generate dummy data with Faker
4. Render template with Handlebars
5. Launch Puppeteer browser (headless)
6. Load rendered HTML
7. Capture screenshot (if PNG requested)
8. Save files to output directory
9. Store metadata in database
10. Return mockup result
```

## Challenges and Solutions

### Challenge: Headless Browser Performance
Puppeteer can be slow and memory-intensive.

**Solution**: Reuse browser instance and implement browser pooling.

### Challenge: Template Complexity
Complex layouts require sophisticated templates.

**Solution**: Use component-based templates with partials.

## What Else Can Be Added
1. More template categories (dashboards, forms, charts)
2. Interactive mockup editor
3. Version control for mockups
4. Mockup comparison and diff
5. Export to design tools (Figma, Sketch)

## API Reference

```typescript
ipcRenderer.invoke('generate-visual-mockup', request: MockupRequest): Promise<MockupResult>
ipcRenderer.invoke('generate-html-prototype', spec: ProtoSpec): Promise<string>
ipcRenderer.invoke('list-mockup-templates'): Promise<Template[]>
```

## Related Documentation
- [Rule Management](09-rule-management.md)
- [Task Management](02-task-management.md)
