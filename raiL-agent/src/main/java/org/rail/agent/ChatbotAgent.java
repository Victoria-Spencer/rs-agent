/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rail.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ReadFileTool;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import jakarta.annotation.Resource;
import org.rail.agent.model.ai.AiStationQueryReq;
import org.rail.agent.model.ai.AiTicketQueryReq;
import org.rail.agent.tools.common.PythonTool;
import org.rail.agent.tools.StationQueryFunction;
import org.rail.agent.tools.TicketQueryFunction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;


@Configuration
public class ChatbotAgent {

	private static final String INSTRUCTION = """
			你是专业的高铁票查询助手。
			1. 用户问车票信息，自动调用 query_ticket 工具查询
			2. 缺少出发地、目的地、日期时，主动追问用户
			3. 回答简洁、清晰、友好
			4. 可使用 Python 工具计算票价、数量
			
			
			【关于 query_ticket 工具的参数 - 生死攸关】
			- 除非用户明确说"高铁"、"动车"、"普速"，否则绝对不要设置 trainTypeIds
			- 除非用户明确说"商务座"、"一等座"、"二等座"，否则绝对不要设置 seatTypes
			- 乱传参数会导致查不到票，禁止自作主张添加参数！
			""";

	private final StationQueryFunction stationQueryFunction;
	private final TicketQueryFunction ticketQueryFunction;
	public ChatbotAgent(StationQueryFunction stationQueryFunction, TicketQueryFunction ticketQueryFunction) {
		this.stationQueryFunction = stationQueryFunction;
		this.ticketQueryFunction = ticketQueryFunction;
	}


	@Bean
	public ReactAgent chatbotReactAgent(ChatModel chatModel,
			ToolCallback executeShellCommand,
			ToolCallback executePythonCode,
			ToolCallback viewTextFile,
			MemorySaver memorySaver) {
		return ReactAgent.builder()
				.name("TicketAgent")
				.model(chatModel)
				.instruction(INSTRUCTION)
				.enableLogging(true)
				.saver(memorySaver)
				// Must set ShellToolAgentHook to manage shell session lifecycle for executeShellCommand
				.hooks(ShellToolAgentHook.builder().shellToolName(executeShellCommand.getToolDefinition().name()).build())
				.tools(
						executeShellCommand,
						executePythonCode,
						viewTextFile,
						stationQueryTool(),
						queryTicketTool()
				)
				.build();
	}

	@Bean
	public MemorySaver memorySaver() {
		return new MemorySaver();
	}

	// Tool: execute_shell_command
	@Bean
	public ToolCallback executeShellCommand() {
		// Use ShellTool with a temporary workspace directory
		String workspaceRoot = System.getProperty("java.io.tmpdir") + File.separator + "agent-workspace";
		return ShellTool.builder(workspaceRoot)
				.withName("execute_shell_command")
				.withDescription("Execute a shell command inside a persistent session. Before running a command, " +
						"confirm the working directory is correct (e.g., inspect with `ls` or `pwd`) and ensure " +
						"any parent directories exist. Prefer absolute paths and quote paths containing spaces, " +
						"such as `cd \"/path/with spaces\"`. Chain multiple commands with `&&` or `;` instead of " +
						"embedding newlines. Avoid unnecessary `cd` usage unless explicitly required so the " +
						"session remains stable. Outputs may be truncated when they become very large, and long " +
						"running commands will be terminated once their configured timeout elapses.")
				.build();
	}

	// Tool: execute_python_code
	@Bean
	public ToolCallback executePythonCode() {
		return FunctionToolCallback.builder("execute_python_code", new PythonTool())
				.description(PythonTool.DESCRIPTION)
				.inputType(PythonTool.PythonRequest.class)
				.build();
	}

	// Tool: view_text_file
	@Bean
	public ToolCallback viewTextFile() {
		// Create a custom wrapper to match the original tool name
		ReadFileTool readFileTool = new ReadFileTool();
		return FunctionToolCallback.builder("view_text_file", readFileTool)
				.description("View the contents of a text file. The file_path parameter must be an absolute path. " +
						"You can specify offset and limit to read specific portions of the file. " +
						"By default, reads up to 500 lines starting from the beginning of the file.")
				.inputType(ReadFileTool.ReadFileRequest.class)
				.build();
	}

	// Tool: station_query
	@Bean
	public ToolCallback stationQueryTool() {
		return FunctionToolCallback.builder("station_query", stationQueryFunction)
				.description("根据站点名称/拼音查询站点编码")
				.inputType(AiStationQueryReq.class)
				.build();
	}

	// Tool: query_ticket
	@Bean
	public ToolCallback queryTicketTool() {
		return FunctionToolCallback.builder("query_ticket", ticketQueryFunction)
				.description("根据出发站编码、到达站编码、出发日期查询高铁票信息")
				.inputType(AiTicketQueryReq.class)
				.build();
	}

}

