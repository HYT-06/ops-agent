package com.ticket.ops.analyzer.rag;

import com.ticket.ops.common.model.EvidenceRef;
import com.ticket.ops.common.model.SceneType;

import java.util.List;

/**
 * P0 空检索器：保证框架可启动，后续替换为 AnalyticDB / Milvus 实现。
 */
public class NoOpRagRetriever implements RagRetriever {

    @Override
    public List<EvidenceRef> retrieve(SceneType scene, String rootCause, int topK) {
        return List.of();
    }
}
