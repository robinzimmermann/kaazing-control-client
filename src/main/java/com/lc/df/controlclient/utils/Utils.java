package com.lc.df.controlclient.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lc.df.controlclient.core.EntityCache;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Pattern;


public class Utils {
    private final static ObjectMapper mapper = new ObjectMapper();

    private static final Pattern MARKET_KEY_PARENTS_PATTERN = Pattern.compile(".+:m.(\\d+)");

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Logger.logErrorMessage("Exception in sleeping current thread", e);
        }

    }

    public static boolean acceptEventMessage(JsonNode sportsbookValue) {
        return !sportsbookValue.path("event").isMissingNode();
    }

    public static boolean acceptMarketMessage(JsonNode sportsbookValue) {
        return !sportsbookValue.path("market").isMissingNode();
    }

    public static boolean acceptSelectionMessage(JsonNode sportsbookValue) {
        return !sportsbookValue.path("selection").isMissingNode();
    }

    public static boolean acceptEventCreateMessage(JsonNode eventNode) {
        return "create".equals(eventNode.path("meta").path("operation").asText(null));
    }

    public static boolean acceptEventUpdateMessage(JsonNode eventNode) {
        return "update".equals(eventNode.path("meta").path("operation").asText(null)) &&
                !eventNode.path("isEventFinished").asBoolean(false);
    }

    public static boolean acceptInplayEventMessage(JsonNode eventNode) {
        return eventNode.path("isEventStarted").asBoolean(false) &&
                !eventNode.path("isEventFinished").asBoolean(false);
    }

    public static boolean acceptEventDeleteMessage(JsonNode eventNode) {
        return "update".equals(eventNode.path("meta").path("operation").asText(null)) &&
                eventNode.path("isEventFinished").asBoolean(false);
    }

    public static JsonNode applyEventCreateForEventsTopic(
            EntityCache entityCache,
            JsonNode eventNode) {
        String categoryJson = entityCache.getCategory(eventNode.path("meta").path("categoryKey")
                .asText("NOTSET"), null);
        JsonNode categorySnapshot = getJsonNode(categoryJson);
        String categoryName = "";
        if (categorySnapshot != null) {
            categoryName = categorySnapshot.path("category").path("categoryName").asText();
        }
        String classJson = entityCache.getClass(eventNode.path("meta").path("classKey")
                .asText("NOTSET"), null);
        JsonNode classSnapshot = getJsonNode(classJson);
        String className = "";
        if (classSnapshot != null) {
            className = classSnapshot.path("class").path("className").asText();
        }
        String typeJson = entityCache.getType(eventNode.path("meta").path("typeKey")
                .asText("NOTSET"), null);
        JsonNode typeSnapshot = getJsonNode(typeJson);
        String typeName = "";
        if (typeSnapshot != null) {
            typeName = typeSnapshot.path("type").path("typeName").asText();
        }

        String eventKey = eventNode.path("eventKey").asText();
        // Create a new event snapshot
        return mapper.createObjectNode()
                .set("event", mapper.createObjectNode()
                        .put("eventKey", eventKey)
                        .put("displayOrder", eventNode.path("displayOrder").asText())
                        .put("eventStatus", eventNode.path("eventStatus").asText())
                        .put("displayStatus", eventNode.path("displayStatus").asText())
                        .put("eventDateTime", eventNode.path("eventDateTime").asText())
                        .put("eventName", eventNode.path("eventName").asText())
                        .put("categoryName", categoryName)
                        .put("className", className)
                        .put("typeName", typeName)
                        .put("ts", getTSString(eventNode))
                        .set("markets", mapper.createArrayNode()));
    }

    public static JsonNode getJsonNode(String json) {
        try {
            if (isValidString(json)) {
                return mapper.readTree(json);
            }
        } catch (IOException e) {
            Logger.logErrorMessage("Error parsing json", e);
        }
        return null;
    }

    public static String getTSString(JsonNode node) {
        String tsString = "";
        String obTimestamp = node.path("meta").path("recordModifiedTime").asText();
        if (obTimestamp != null && obTimestamp.length() > 0) {
            tsString += ",o." + obTimestamp;
        }
        tsString += ",s." + System.currentTimeMillis();
        String kafkaTimestamp = node.path("meta").path("messageTimestamp").asText();
        if (kafkaTimestamp != null & kafkaTimestamp.length() > 0) {
            tsString += ",k." + kafkaTimestamp;
        }
        return tsString.substring(1);
    }

    public static boolean isValidString(String string) {
        return string != null && string.length() > 0;
    }

    public static JsonNode applyEventUpdateForEventsTopic(
            JsonNode eventNode,
            JsonNode eventSnapshot) {
        if (eventSnapshot != null) {
            // Merge the delta into snapshot, except for the meta section
            final Iterator<Entry<String, JsonNode>> deltaFields = eventNode.fields();
            while (deltaFields.hasNext()) {
                final Entry<String, JsonNode> field = deltaFields.next();
                Object obj = eventSnapshot.path("event");
                if (obj instanceof ObjectNode) {
                    final ObjectNode snapshotFields = (ObjectNode) obj;
                    if (!"meta".equals(field.getKey())) {
                        if (!"eventKey".equals(field.getKey())) {
                            snapshotFields.set(field.getKey(), field.getValue());
                            snapshotFields.put("ts", getTSString(eventNode));
                        }
                    }
                }
            }
            return eventSnapshot;
        }
        return null;
    }

    public static boolean acceptMarketCreateMessage(JsonNode marketNode) {
        return "create".equals(marketNode.path("meta").path("operation").asText(null));
    }

    public static boolean acceptMarketUpdateMessage(JsonNode marketNode) {
        return "update".equals(marketNode.path("meta").path("operation").asText(null)
        ) && !marketNode.path("isSettled").asBoolean(false);
    }

    public static boolean acceptMarketDeleteMessage(JsonNode marketNode) {
        return "update".equals(marketNode.path("meta").path("operation").asText(null)) &&
                marketNode.path("isSettled").asBoolean(false);
    }

    public static boolean acceptSelectionCreateMessage(JsonNode selectionNode) {
        return "create".equals(selectionNode.path("meta").path("operation").asText(null));
    }

    public static boolean acceptSelectionUpdateMessage(JsonNode selectionNode) {

        return "update".equals(selectionNode.path("meta").
                path("operation").asText(null)) &&
                !selectionNode.
                        path("isSettled").asBoolean(false);

    }

    public static boolean acceptSelectionDeleteMessage(JsonNode selectionNode) {
        return "update".equals(selectionNode.path("meta").path("operation").asText(null)) &&
                selectionNode.path("isSettled").asBoolean(false);
    }

    public static JsonNode applyMarketCreateForEventsTopic(JsonNode marketNode, JsonNode eventSnapshot) {
        String marketKey = marketNode.path("marketKey").asText(null);
        if (eventSnapshot != null) {
            JsonNode eventSnapshotNode = eventSnapshot.path("event");
            JsonNode markets = eventSnapshotNode.path("markets");
            if (markets instanceof ArrayNode) {
                ArrayNode snapshotMarkets = (ArrayNode) markets;

                // Check if the market already exists. If so, override it.
                ObjectNode snapshotMarket;
                int marketIndex = -1;
                for (int i = 0; i < snapshotMarkets.size(); i++) {
                    JsonNode m = snapshotMarkets.get(i);
                    if (m.path("marketKey").asText(null).equals(marketKey)) {
                        marketIndex = i;
                        break;
                    }
                }
                if (marketIndex >= 0) {
                    snapshotMarket = (ObjectNode) snapshotMarkets.get(marketIndex);
                } else {
                    snapshotMarket = mapper.createObjectNode();
                }

                snapshotMarket
                        .put("marketKey", marketKey)
                        .put("displayOrder", marketNode.path("displayOrder").asLong(0));

                if (marketIndex == -1) {
                    snapshotMarkets.add(snapshotMarket);
                }
            }
            if (eventSnapshotNode instanceof ObjectNode) {
                ((ObjectNode) eventSnapshotNode).put("ts", getTSString(marketNode));
            }
            return eventSnapshot;
        }
        return null;
    }

    public static JsonNode applyMarketCreateForMarketsTopic(
            JsonNode marketNode) {
        // Create a new market snapshot
        return mapper.createObjectNode()
                .set("market", mapper.createObjectNode()
                        .put("marketKey", marketNode.path("marketKey").asText())
                        .put("marketName", marketNode.path("marketName").asText())
                        .put("displayOrder", marketNode.path("displayOrder").asLong(0))
                        .put("ts", getTSString(marketNode))
                        .set("selections", mapper.createArrayNode()));
    }

    public static JsonNode applyMarketUpdateForMarketsTopic(
            JsonNode marketNode,
            JsonNode marketSnapshot) {
        if (marketSnapshot != null) {
            // Merge the delta into snapshot, except for the meta section
            final Iterator<Entry<String, JsonNode>> deltaFields = marketNode.fields();
            while (deltaFields.hasNext()) {
                final Entry<String, JsonNode> field = deltaFields.next();
                Object obj = marketSnapshot.path("market");
                if (obj instanceof ObjectNode) {
                    final ObjectNode snapshotFields = (ObjectNode) obj;
                    if (!"meta".equals(field.getKey())) {
                        if (!"marketKey".equals(field.getKey())) {
                            snapshotFields.set(field.getKey(), field.getValue());
                            snapshotFields.put("ts", getTSString(marketNode));
                        }
                    }
                }
            }
            return marketSnapshot;
        }
        return null;
    }

    public static JsonNode applySelectionCreateForMarketsTopic(
            JsonNode selectionNode,
            JsonNode marketSnapshot) {
        String selectionKey = selectionNode.path("selectionKey").asText(null);
        if (marketSnapshot != null) {
            JsonNode marketSnapshotNode = marketSnapshot.path("market");
            JsonNode selections = marketSnapshotNode.path("selections");
            if (selections instanceof ArrayNode) {
                ArrayNode snapshotSelections = (ArrayNode) selections;

                // Check if the selection already exists. If so, override it.
                ObjectNode snapshotSelection;
                int selectionIndex = -1;
                for (int i = 0; i < snapshotSelections.size(); i++) {
                    JsonNode s = snapshotSelections.get(i);

                    if (s.path("selectionKey").asText(null).equals(selectionKey)) {
                        selectionIndex = i;
                        break;
                    }
                }
                if (selectionIndex >= 0) {
                    snapshotSelection = (ObjectNode) snapshotSelections.get(selectionIndex);
                } else {
                    snapshotSelection = mapper.createObjectNode();
                }

                snapshotSelection
                        .put("selectionKey", selectionKey)
                        .put("selectionName", selectionNode.path("selectionName").asText(null))
                        .put("displayOrder", selectionNode.path("displayOrder").asLong(0));

                if (selectionIndex == -1) {
                    snapshotSelections.add(snapshotSelection);
                }
            }
            return marketSnapshot;
        }
        return null;
    }

    public static JsonNode applySelectionCreateForSelectionsTopic(
            JsonNode selectionNode) {
        // Create a new selection snapshot
        ObjectNode newSelectionSnapshot = (ObjectNode) mapper.createObjectNode()
                .set("selection", mapper.createObjectNode()
                        .put("selectionKey", selectionNode.path("selectionKey").asText())
                        .put("selectionName", selectionNode.path("selectionName").asText())
                        .put("selectionStatus", selectionNode.path("selectionStatus").asText())
                        .put("displayStatus", selectionNode.path("displayStatus").asText())
                        .put("displayOrder", selectionNode.path("displayOrder").asLong())
                        .put("ts", getTSString(selectionNode)));

        ObjectNode snapshotSelectionNode = (ObjectNode) newSelectionSnapshot.path("selection");
        JsonNode priceArray = selectionNode.path("prices").path("price");
        if (!priceArray.isMissingNode()) {
            JsonNode price = ((ArrayNode) priceArray).get(0);
            snapshotSelectionNode
                    .put("numPrice", price.path("numPrice").asLong(0))
                    .put("denPrice", price.path("denPrice").asLong(0));
        }
        // Create a new selection snapshot
        return newSelectionSnapshot;
    }

    public static JsonNode applySelectionUpdateForSelectionsTopic(
            JsonNode selectionNode,
            JsonNode selectionSnapshot) {
        if (selectionSnapshot != null) {
            Object obj = selectionSnapshot.path("selection");
            if (obj instanceof ObjectNode) {
                ObjectNode snapshotSelectionNode = (ObjectNode) obj;

                JsonNode priceArray = selectionNode.path("prices").path("price");
                if (!priceArray.isMissingNode()) {
                    JsonNode price = ((ArrayNode) priceArray).get(0);
                    snapshotSelectionNode
                            .put("numPrice", price.path("numPrice").asLong(0))
                            .put("denPrice", price.path("denPrice").asLong(0));
                }
                if (!selectionNode.path("selectionStatus").isMissingNode()) {
                    snapshotSelectionNode.put("selectionStatus", selectionNode.path("selectionStatus").asText());
                }
                if (!selectionNode.path("displayStatus").isMissingNode()) {
                    snapshotSelectionNode.put("displayStatus", selectionNode.path("displayStatus").asText());
                }
                if (!selectionNode.path("isResulted").isMissingNode()) {
                    snapshotSelectionNode.put("isResulted", selectionNode.path("isResulted").asBoolean());
                }
                if (!selectionNode.path("isSettled").isMissingNode()) {
                    snapshotSelectionNode.put("isSettled", selectionNode.path("isSettled").asBoolean());
                }
                if (!selectionNode.path("displayOrder").isMissingNode()) {
                    snapshotSelectionNode.put("displayOrder", selectionNode.path("displayOrder").asLong());
                }
                snapshotSelectionNode.put("ts", getTSString(selectionNode));

                return selectionSnapshot;
            }
        }
        return null;
    }

    public static String getJsonString(JsonNode jsonNode) {
        try {

            return mapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            Logger.logErrorMessage("Error rendering json", e);
        }
        return null;
    }
}
