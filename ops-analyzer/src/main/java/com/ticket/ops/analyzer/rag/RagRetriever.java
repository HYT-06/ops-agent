package com.ticket.ops.analyzer.rag;

import com.ticket.ops.common.model.EvidenceRef;
import com.ticket.ops.common.model.SceneType;

import java.util.List;

/**
 * 可信区知识检索接口。P4 接入向量库，当前为空实现。
 */
public interface RagRetriever {

    List<EvidenceRef> retrieve(SceneType scene, String rootCause, int topK);
}
