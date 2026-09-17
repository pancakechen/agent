package com.api.aiagent.app;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;

/**
 * 实现智能体的思考和行动
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Log4j2
public abstract class ReActAgent extends BaseAgent {


    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动 true执行 false 不执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     * @return 行动执行结果
     */
    public abstract String act();


    /**
     * 执行单个步骤 先思考再行动
     * @return 步骤执行结果
     */
/*
    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "思考完成 无需行动";
            }
            return act();
        }catch (Exception e){
            log.error(e.getMessage());
            return "步骤执行失败";
        }

    }
*/

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return getFinalAnswer();
            }
            return act();
        }catch (Exception e){
            setAgentState(AgentState.ERROR);
            setFinalAnswer("步骤执行失败：" + e.getMessage());
            log.error("步骤执行失败：" + e.getMessage());
            return getFinalAnswer();
        }
    }
}
