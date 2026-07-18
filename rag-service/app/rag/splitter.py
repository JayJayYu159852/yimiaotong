"""文本切分：递归字符切分，中文分隔符优先级"""
from langchain_text_splitters import RecursiveCharacterTextSplitter

# chunk_size 500 字 + overlap 80 字：
# 过小 -> 上下文碎片化引用价值低；过大 -> 检索粒度粗且挤占 prompt 空间
_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=80,
    separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""],
    keep_separator="end",
)


def split_text(text: str) -> list[str]:
    return [chunk.strip() for chunk in _splitter.split_text(text) if chunk.strip()]
