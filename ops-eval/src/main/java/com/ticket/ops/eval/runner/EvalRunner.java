package com.ticket.ops.eval.runner;

import com.ticket.ops.eval.dataset.EvalCase;
import com.ticket.ops.eval.dataset.EvalDataset;

import java.util.List;

/**
 * P0 评测入口：验证评测集可加载。P4 接入分析 Agent 与指标计算。
 */
public final class EvalRunner {

    private EvalRunner() {
    }

    public static void main(String[] args) {
        List<EvalCase> cases = EvalDataset.defaultCases();
        System.out.println("ops-agent 评测集加载成功，共 " + cases.size() + " 条");
        for (EvalCase evalCase : cases) {
            System.out.printf("- %s [%s] %s%n",
                    evalCase.caseId(), evalCase.scene(), evalCase.triggerDescription());
        }
    }
}
