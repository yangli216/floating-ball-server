package com.regionalai.floatingball.server.modules.emrtemplate.dto;

import lombok.Data;

@Data
public class InpatientEmrTemplatePromptGenerateVO {

    private String prompt;

    private String generatorInstruction;
}
