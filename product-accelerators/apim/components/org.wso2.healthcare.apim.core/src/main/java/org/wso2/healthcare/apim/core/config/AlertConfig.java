/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.core.config;

/**
 * Contains alert related config
 */
public class AlertConfig {

    private AlertType alertType = AlertType.LOG;
    private LogFormat logFormat = LogFormat.STRUCTURED;
    private boolean escalationEnable = false;
    private WorkflowConfig escalationWorkflowConfig;

    public AlertConfig() {
    }

    public WorkflowConfig getEscalationWorkflowConfig() {
        return escalationWorkflowConfig;
    }

    public void setEscalationWorkflowConfig(WorkflowConfig escalationWorkflowConfig) {
        this.escalationWorkflowConfig = escalationWorkflowConfig;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public boolean isEscalationEnable() {
        return escalationEnable;
    }

    public void setEscalationEnable(boolean escalationEnable) {
        this.escalationEnable = escalationEnable;
    }

    public LogFormat getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(LogFormat logFormat) {
        this.logFormat = logFormat;
    }

    /**
     * Available alert types
     */
    public enum AlertType {
        LOG("log"),
        PUBLISH("publish");

        private final String value;

        AlertType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static AlertType fromValue(String v) {
            for (AlertType b : AlertType.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(v)) {
                    return b;
                }
            }
            return null;
        }
    }

    /**
     * Logging format
     */
    public enum LogFormat {
        STRUCTURED("structured"),
        JSON("json");

        private final String value;

        LogFormat(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static LogFormat fromValue(String v) {
            for (LogFormat b : LogFormat.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(v)) {
                    return b;
                }
            }
            return null;
        }
    }
}
