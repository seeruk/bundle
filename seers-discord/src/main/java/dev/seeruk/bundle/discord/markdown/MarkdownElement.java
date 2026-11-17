package dev.seeruk.bundle.discord.markdown;

/**
 * Additional Discord Markdown elements which aren't provided by SimpleAST, (see
 * {@link MarkdownParser}).
 */
sealed interface MarkdownElement {
    // The contents of code blocks are not parsed, they come through as-is.
    record CodeBlock() implements MarkdownElement {}

    record Code() implements MarkdownElement {}

    // The contents of these may be parsed:
    record Spoiler() implements MarkdownElement {}

    record Quote() implements MarkdownElement {}

    record Heading() implements MarkdownElement {}

    record SmallText() implements MarkdownElement {}

    record Link(String url) implements MarkdownElement {}

    // Mentions are terminal elements, so the contents are not parsed.
    record Mention(MentionKind kind, String id) implements MarkdownElement {}

    enum MentionKind {USER, ROLE, CHANNEL}
}
