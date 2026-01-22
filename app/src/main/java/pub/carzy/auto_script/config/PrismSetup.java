package pub.carzy.auto_script.config;

import io.noties.prism4j.annotations.PrismBundle;

@PrismBundle(
        include = {"java", "kotlin", "javascript", "json", "markup"},
        grammarLocatorClassName = ".MyGrammarLocator"
)
class PrismSetup {}
