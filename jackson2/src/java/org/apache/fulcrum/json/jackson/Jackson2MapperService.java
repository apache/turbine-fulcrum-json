package org.apache.fulcrum.json.jackson;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.commons.lang3.StringUtils;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.filters.CustomModuleWrapper;
import org.apache.fulcrum.json.jackson.jsonpath.DefaultJsonPathWrapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.ConfigFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * 
 * 
 * By default multiple serialization of the same object in a single thread is
 * not supported (e.g filter + mixin or default + filter for the same bean /
 * object).
 * 
 * By default a filter is defined by its {@link Class#getName()}.
 * 
 * Note: If using {@link SimpleNameIntrospector}, filter caches are set by class
 * id. Caching is enabled by default, if not (a) by setting
 * {@link #cacheFilters} to <code>false</code>. By setting (b) the Boolean
 * parameter clean
 * {@link #serializeAllExceptFilter(Object, Class, Boolean, String...)} or
 * {@link #serializeOnlyFilter(Object, Class, Boolean, String...)} you could
 * clean the filter. If caching is disabled each filter will be unregistered and
 * the cache cleaned.
 * 
 * @author <a href="mailto:gk@apache.org">Georg Kallidis</a>
 * @version $Id$
 * 
 */
public class Jackson2MapperService extends AbstractLogEnabled implements JsonService, Initializable, Configurable {

	private static final String DEFAULT_TYPING = "defaultTyping";
	private static final String CACHE_FILTERS = "cacheFilters";
	private static final String DATE_FORMAT = "dateFormat";
	private static final String ESCAPE_CHARS = "escapeCharsGlobal";
	private static final String ESCAPE_CHAR_CLASS = "escapeCharsClass";
	private static final String USE_JSON_PATH = "useJsonPath";
	ObjectMapper mapper;
	AnnotationIntrospector primary; // support default
	AnnotationIntrospector secondary;

	private static final String ANNOTATIONINSPECTOR = "annotationInspectors";

	private Map<String, String> annotationInspectors = null;
	private Map<String, Boolean> features = null;
	private Map<String, String> featureTypes = null;

	private String dateFormat;

	/**
	 * Default dateformat is <code>MM/dd/yyyy</code>, could be overwritten in
	 * {@link #setDateFormat(DateFormat)}.
	 */
	public static final String DEFAULTDATEFORMAT = "MM/dd/yyyy";

	private boolean cacheFilters = true; // true -> this is by default true in jackson, if not using
											// multiple serialization in one thread
	String[] defaultTypeDefs = null;
	private CacheService cacheService;
	private boolean escapeCharsGlobal = false; // to be backward compatible, but should be true, then escaping to avoid
												// XSS payload by default
	private boolean useJsonPath = false;
	private String escapeCharsClass = null;

	@Override
	public String ser(Object src) throws Exception {
		return ser(src, false);
	}

	@Override
	public <T> String ser(Object src, Class<T> type) throws Exception {
		return ser(src, type, false);
	}

	public String ser(Object src, FilterProvider filter) throws Exception {
		return ser(src, filter, false);
	}

	public String ser(Object src, FilterProvider filter, Boolean cleanCache) throws Exception {
		String serResult = null;
		if (src == null) {
			getLogger().info("no serializable object.");
			return serResult;
		}
		if (filter == null) {
			getLogger().debug("ser class::" + src.getClass() + " without filter.");
			return ser(src);
		} else {
			getLogger().debug("add filter for cache filter Class " + src.getClass().getName());
			setCustomIntrospectorWithExternalFilterId(src.getClass(), null); // filter class
			cacheService.getFilters().put(src.getClass().getName(), filter);
		}
		getLogger().debug("ser class::" + src.getClass() + " with filter " + filter);
		mapper.setFilterProvider(filter);
		String res = mapper.writer(filter).writeValueAsString(src);
		if (cleanCache) {
			cacheService.cleanSerializerCache(mapper);
		}
		return res;
	}

	@Override
	public <T> T deSer(String json, Class<T> type) throws Exception {
		ObjectReader reader = null;
		if (type != null)
			reader = mapper.readerFor(type);
		else
			reader = mapper.reader();

		return reader.readValue(json);
	}

	public <T> T deSer(Object src, Class<T> type) throws Exception {
		return mapper.convertValue(src, type);
	}

	public <T> Collection<T> deSerCollectionWithType(String json, Class<? extends Collection> collectionClass,
			Class<T> type) throws Exception {
		return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(collectionClass, type));
	}

	public <T> String serCollectionWithTypeReference(Collection<T> src, TypeReference collectionType,
			Boolean cleanCache) throws Exception {
		String res = mapper.writerFor(collectionType).writeValueAsString(src);
		if (cleanCache) {
			cacheService.cleanSerializerCache(mapper);
		}
		return res;
	}

	@Override
	public <T> Collection<T> deSerCollection(String json, Object collectionType, Class<T> elementType)
			throws Exception {
		if (collectionType instanceof TypeReference) {
			return mapper.readValue(json, (TypeReference<T>) collectionType);
		} else {
			return mapper.readValue(json, mapper.getTypeFactory()
					.constructCollectionType(((Collection<T>) collectionType).getClass(), elementType));
		}
	}

	public <T> List<T> deSerList(String json, Class<? extends List> targetList, Class<T> elementType) throws Exception {
		return mapper.readValue(json, mapper.getTypeFactory().constructParametricType(targetList, elementType));
	}

	public <T, U> Map<T, U> deSerMap(String json, Class<? extends Map> mapClass, Class<T> keyClass, Class<U> valueClass)
			throws Exception {
		return mapper.readValue(json, mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass));
	}

	public <T> Collection<T> deSerCollectionWithTypeReference(String json, TypeReference<T> collectionType)
			throws Exception {
		return mapper.readValue(json, collectionType);
	}

	/**
	 * @param name   name of the module
	 * @param target target class
	 * @param mixin  provide mixin as class. Deregistering module could be only done
	 *               by setting this parameter to null.
	 * 
	 * @see #addAdapter(String, Class, Object)
	 */
	@Override
	public JsonService addAdapter(String name, Class target, Class mixin) throws Exception {
		getLogger().debug(
				"registering unversioned simple mixin module named " + name + " of type " + mixin + "  for: " + target);
		mapper.addMixIn(target, mixin);
		return this;
	}

	/**
	 * Add a named module or a {@link Module}.
	 * 
	 * @param name   Name of the module, optional. Could be null, if module is a
	 *               {@link Module}.
	 * 
	 * @param target Target class, optional. Could be null, if module is a
	 *               {@link Module}.
	 * 
	 * @param module Either an Jackson Module @link {@link Module} or an custom
	 *               wrapper @link CustomModuleWrapper.
	 * 
	 * @see JsonService#addAdapter(String, Class, Object)
	 */
	@Override
	public JsonService addAdapter(String name, Class target, Object module) throws Exception {
		if (module instanceof CustomModuleWrapper) {
			CustomModuleWrapper cmw = (CustomModuleWrapper) module;
			Module cm = new CustomModule(name, target, cmw.getSer(), cmw.getDeSer());
			getLogger().debug("registering custom module " + cm + "  for: " + target);
			mapper.registerModule(cm);
		} else if (module instanceof Module) {
			getLogger().debug("registering module " + module);
			mapper.registerModule((Module) module);
		} else {
			throw new Exception("expecting module type" + Module.class);
		}
		return this;
	}

	/**
	 * set a single mixin. convenience method, calls
	 * {@link ObjectMapper#registerModule(Module)}
	 * 
	 * @param src    he object to be serialized
	 * @param name   the name for the mixin
	 * @param target the target class for the mixin
	 * @param mixin  the mixin class
	 * @return serialized result
	 * @throws JsonProcessingException if not properly processed
	 */
	@SuppressWarnings("rawtypes")
	public String withMixinModule(Object src, String name, Class target, Class mixin) throws JsonProcessingException {
		Module mx = new MixinModule(name, target, mixin);
		getLogger().debug("registering module " + mx + ", mixin: " + mixin);
		return mapper.registerModule(mx).writer().writeValueAsString(src);
	}

	/**
	 * This is a convenience method with read, but the old mixins will be cleared
	 * {@link ObjectMapper#setMixIns(Map)}
	 * 
	 * @param src    the object to be serialized
	 * @param target the target class for the mixin
	 * @param mixin  the mixin class
	 * @return serialized result
	 * @throws JsonProcessingException if fail
	 */
	@SuppressWarnings("rawtypes")
	public String withSetMixins(Object src, Class target, Class mixin) throws JsonProcessingException {
		return setMixins(target, mixin).writer().writeValueAsString(src);
	}

	/**
	 * @param target The target class
	 * @param mixin  the mixin class
	 * @return an objectmapper
	 */
	@SuppressWarnings("rawtypes")
	public ObjectMapper setMixins(Class target, Class mixin) {
		Map<Class<?>, Class<?>> sourceMixins = null;
		if (target != null) {
			sourceMixins = new HashMap<>(1);
			sourceMixins.put(target, mixin);
		}
		getLogger().debug("clean set mixins for target " + target + ", mixin: " + mixin);
		return mapper.setMixIns(sourceMixins);
	}

	@Override
	public String serializeAllExceptFilter(Object src, String... filterAttr) throws Exception {
		return serializeAllExceptFilter(src, src.getClass(), true, filterAttr);
	}

	@Override
	public synchronized String serializeAllExceptFilter(Object src, Boolean cache, String... filterAttr)
			throws Exception {
		return serializeAllExceptFilter(src, src.getClass(), cache, filterAttr);
	}

	public synchronized <T> String serializeAllExceptFilter(Object src, Class<T>[] filterClasses, String... filterAttr)
			throws Exception {
		return serializeAllExceptFilter(src, filterClasses, true, filterAttr);
	}

	@Override
	public synchronized <T> String serializeAllExceptFilter(Object src, Class<T> filterClass, String... filterAttr)
			throws Exception {
		return serializeAllExceptFilter(src, filterClass, true, filterAttr);
	}

	@Override
	public <T> String serializeAllExceptFilter(Object src, Class<T> filterClass, Boolean cleanFilter,
			String... filterAttr) throws Exception {
		return serializeAllExceptFilter(src, new Class[] { filterClass }, cleanFilter, filterAttr);
	}

	/**
	 * 
	 * @param src           the object to be serailized, may be a list
	 * @param filterClasses the same object class or a detail class, which should be
	 *                      filtered
	 * @param               <T> class type
	 * @param clean         cleaning the cache after serialization
	 * @param filterAttr    attributes to be filtered for filtered class
	 * @return the serailized string
	 * @throws Exception generic exception
	 */
	public synchronized <T> String serializeAllExceptFilter(Object src, Class<T>[] filterClasses, Boolean clean,
			String... filterAttr) throws Exception {
		PropertyFilter pf = null;
		if (filterAttr != null)
			pf = SimpleBeanPropertyFilter.serializeAllExcept(filterAttr);
		else if (filterClasses == null) { // no filter
			return ser(src, clean);
			// should be better:
			// return filter(src, new Class<?>[] { src.getClass() }, filterClasses, pf,
			// clean);
		}
		return filter(src, new Class<?>[] { filterClasses[0] }, filterClasses, pf, clean);
	}

	@Override
	public String serializeOnlyFilter(Object src, String... filterAttrs) throws Exception {
		return serializeOnlyFilter(src, src.getClass(), true, filterAttrs);
	}

	@Override
	public synchronized String serializeOnlyFilter(Object src, Boolean cache, String... filterAttr) throws Exception {
		return serializeOnlyFilter(src, src.getClass(), cache, filterAttr);
	}

	@Override
	public synchronized <T> String serializeOnlyFilter(Object src, Class<T> filterClass, String... filterAttr)
			throws Exception {
		return serializeOnlyFilter(src, filterClass, true, filterAttr);
	}

	@Override
	public synchronized <T> String serializeOnlyFilter(Object src, Class<T> filterClass, Boolean refresh,
			String... filterAttr) throws Exception {
		return serializeOnlyFilter(src, new Class[] { filterClass }, refresh, filterAttr);
	}

	public synchronized <T> String serializeOnlyFilter(Object src, Class<T>[] filterClasses, Boolean refresh,
			String... filterAttr) throws Exception {
		PropertyFilter pf = null;
		if (filterAttr != null && filterAttr.length > 0 && !"".equals(filterAttr[0])) {
			pf = SimpleBeanPropertyFilter.filterOutAllExcept(filterAttr);
			getLogger().debug("setting filteroutAllexcept filter for size of filterAttr: " + filterAttr.length);
		} else {
			getLogger().warn("no filter attributes set!");
			pf = SimpleBeanPropertyFilter.filterOutAllExcept("dummy");
		}
		if (filterClasses == null)
			throw new Exception("You have to provide some class to apply the filtering!");
		return filter(src, filterClasses, null, pf, refresh);
	}

	@Override
	public String ser(Object src, Boolean cleanCache) throws Exception {
		if (cacheService.getFilters().containsKey(src.getClass().getName())) {
			getLogger().warn("Found registered filter - using instead of default view filter for class:"
					+ src.getClass().getName());
			SimpleFilterProvider filter = (SimpleFilterProvider) cacheService.getFilters()
					.get(src.getClass().getName());
			return ser(src, filter, cleanCache);// mapper.writerWithView(src.getClass()).writeValueAsString(src);
		}
		String res = mapper.writerWithView(Object.class).writeValueAsString(src);
		if (cleanCache != null && cleanCache) {
			cacheService.cleanSerializerCache(mapper);
		}
		return res;
	}

	@Override
	public <T> String ser(Object src, Class<T> type, Boolean cleanCache) throws Exception {
		getLogger().info("serializing object:" + src + " for type " + type);
		if (src != null && cacheService.getFilters().containsKey(src.getClass().getName())) {
			getLogger().warn("Found registered filter - could not use custom view and custom filter for class:"
					+ src.getClass().getName());
			// throw new
			// Exception("Found registered filter - could not use custom view and custom
			// filter for class:"+
			// src.getClass().getName());
			SimpleFilterProvider filter = (SimpleFilterProvider) cacheService.getFilters()
					.get(src.getClass().getName());
			return ser(src, filter);
		}

		String res = (type != null) ? mapper.writerWithView(type).writeValueAsString(src)
				: mapper.writeValueAsString(src);
		if (cleanCache) {
			cacheService.cleanSerializerCache(mapper);
		}
		return res;
	}

	/**
	 * 
	 * @param src            The source Object to be filtered.
	 * @param filterClass    This Class array contains at least one element. If no
	 *                       class is provided it is the class type of the source
	 *                       object. The filterClass is to become the key of the
	 *                       filter object cache.
	 * @param excludeClasses The classes to be excluded, optionally used only for
	 *                       methods like
	 *                       {@link #serializeAllExceptFilter(Object, Class[], String...)}.
	 * @param pf             Expecting a property filter from e.g @link
	 *                       {@link SimpleBeanPropertyFilter}.
	 * @param clean          if <code>true</code> does not reuse the filter object
	 *                       (no cashing).
	 * @return The serialized Object as String
	 * @throws Exception
	 */
	private <T> String filter(Object src, Class<?>[] filterClasses, Class<T>[] excludeClasses, PropertyFilter pf,
			Boolean clean) throws Exception {
		FilterProvider filter = null;
		if (filterClasses.length > 0) {
			filter = retrieveFilter(pf, filterClasses[0], excludeClasses);
		}
		getLogger().info("filtering with filter " + filter);
		String serialized = ser(src, filter, clean);
		if (!cacheFilters || clean) {
			if (filterClasses.length > 0) {
				boolean exclude = (excludeClasses != null) ? true : false;
				cacheService.removeFilter(filterClasses[0], exclude);
			}
		}
		return serialized;
	}

	private <T> SimpleFilterProvider retrieveFilter(PropertyFilter pf, Class<?> filterClass,
			Class<T>[] excludeClasses) {
		SimpleFilterProvider filter = null;
		if (pf != null) {
			filter = new SimpleFilterProvider();
			filter.setDefaultFilter(pf);
		}
		if (!cacheService.getFilters().containsKey(filterClass.getName())) {
			getLogger().debug("add filter for cache filter Class " + filterClass.getName());
			setCustomIntrospectorWithExternalFilterId(filterClass, excludeClasses); // filter class
			if (pf != null) {
				cacheService.getFilters().put(filterClass.getName(), filter);
			}
		} else {
			filter = (SimpleFilterProvider) cacheService.getFilters().get(filterClass.getName());
			// setCustomIntrospectorWithExternalFilterId(filterClass); // filter
			// class
		}
		getLogger().debug("set filter:" + filter);
		return filter;
	}

	/**
	 * @param filterClass
	 *                          <li>Adding filterClass into
	 *                          {@link SimpleNameIntrospector#setFilteredClass(Class)}
	 *                          enables the filtering process.
	 * @param externalFilterIds
	 *                          <li>Adding externalFilterIs to
	 *                          {@link SimpleNameIntrospector#setExternalFilterExcludeClasses(Class...)}
	 *                          excludes these classes.
	 */
	private <T> void setCustomIntrospectorWithExternalFilterId(Class<?> filterClass,
			Class<T>[] externalFilterClassIds) {
		if (primary instanceof SimpleNameIntrospector) {
			// first one is required that we get to the PropertyFilter
			((SimpleNameIntrospector) primary).setFilteredClasses(filterClass);
			if (externalFilterClassIds != null) {
				((SimpleNameIntrospector) primary).setIsExludeType(true);
				for (Class<T> filterClazz : externalFilterClassIds) {
					getLogger().debug("added class for filters " + filterClazz);
				}
				((SimpleNameIntrospector) primary).setExternalFilterExcludeClasses(externalFilterClassIds);
			}
		}
	}

	public Jackson2MapperService registerModule(Module module) {
		mapper.registerModule(module);
		return this;
	}

	public <T> void addSimpleModule(SimpleModule module, Class<T> type, JsonSerializer<T> ser) {
		module.addSerializer(type, ser);
	}

	public <T> void addSimpleModule(SimpleModule module, Class<T> type, JsonDeserializer<T> deSer) {
		module.addDeserializer(type, deSer);
	}

	/**
	 * Default Dateformat: {@link #DEFAULTDATEFORMAT}
	 */
	@Override
	public void setDateFormat(final DateFormat df) {
		mapper.setDateFormat(df);
	}

	/**
	 * Avalon component lifecycle method
	 */
	@Override
	public void configure(Configuration conf) throws ConfigurationException {
		getLogger().debug("conf.getName()" + conf.getName());
		this.annotationInspectors = new HashMap<>();

		final Configuration configuredAnnotationInspectors = conf.getChild(ANNOTATIONINSPECTOR, false);

		if (configuredAnnotationInspectors != null) {
			Configuration[] nameVal = configuredAnnotationInspectors.getChildren();
			for (int i = 0; i < nameVal.length; i++) {
				String key = nameVal[i].getName();
				getLogger().debug("configured key: " + key);
				if (key.equals("features")) {
					this.features = new HashMap<>();
					this.featureTypes = new HashMap<>();
					Configuration[] localFeatures = nameVal[i].getChildren();
					for (int j = 0; j < localFeatures.length; j++) {
						boolean featureValue = localFeatures[j].getAttributeAsBoolean("value", false);
						String featureType = localFeatures[j].getAttribute("type");
						String feature = localFeatures[j].getValue();
						getLogger().debug("configuredAnnotationInspectors " + feature + ":" + featureValue);
						this.features.put(feature, featureValue);
						this.featureTypes.put(feature, featureType);
					}
				} else {
					String val = nameVal[i].getValue();
					getLogger().debug("configuredAnnotationInspectors " + key + ":" + val);
					this.annotationInspectors.put(key, val);
				}
			}
		}
		final Configuration configuredDateFormat = conf.getChild(DATE_FORMAT, true);
		this.dateFormat = configuredDateFormat.getValue(DEFAULTDATEFORMAT);

		final Configuration configuredKeepFilter = conf.getChild(CACHE_FILTERS, false);
		if (configuredKeepFilter != null) {
			this.cacheFilters = configuredKeepFilter.getValueAsBoolean();
		}
		final Configuration configuredEscapeChars = conf.getChild(ESCAPE_CHARS, false);
		if (configuredEscapeChars != null) {
			this.escapeCharsGlobal = configuredEscapeChars.getValueAsBoolean();
		}
		final Configuration configuredEscapeCharClass = conf.getChild(ESCAPE_CHAR_CLASS, false);
		if (configuredEscapeCharClass != null) {
			this.escapeCharsClass = configuredEscapeCharClass.getValue();
		}

		final Configuration configuredDefaultType = conf.getChild(DEFAULT_TYPING, false);
		if (configuredDefaultType != null) {
			defaultTypeDefs = new String[] { configuredDefaultType.getAttribute("type"),
					configuredDefaultType.getAttribute("key") };
		}
		final Configuration configuredjsonPath = conf.getChild(USE_JSON_PATH, false);
		if (configuredjsonPath != null) {
			this.useJsonPath = configuredjsonPath.getValueAsBoolean();
		}
	}

	@Override
	public void initialize() throws Exception {
		mapper = new ObjectMapper(null, null, null);// add configurable JsonFactory,.. later?

		initAnnotationInspectors();

		initFeatures();

		initDefaultTyping();

		getLogger().info("setting date format to:" + dateFormat);
		getLogger().info("cacheFilters is:" + cacheFilters);
		if (!cacheFilters) {
			mapper.configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true);
		}

		mapper.setDateFormat(new SimpleDateFormat(dateFormat));

		if (escapeCharsGlobal) {
			mapper.getFactory().setCharacterEscapes(characterEscapes);
		}
		if (escapeCharsClass != null) {
			try {
				characterEscapes = (CharacterEscapes) Class.forName(escapeCharsClass).getConstructor().newInstance();
			} catch (Exception e) {
				throw new Exception(
						"JsonMapperService: Error instantiating " + escapeCharsClass + " for " + ESCAPE_CHAR_CLASS);
			}
		}

		getLogger().debug("initialized mapper:" + mapper);

		mapper.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
			@Override
			public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
				jgen.writeString("");

			}
		});
		cacheService = new CacheService(primary);
		if (cacheService instanceof LogEnabled) {
			cacheService.enableLogging(getLogger().getChildLogger(cacheService.getClass().getSimpleName()));
			getLogger().info("setting cacheService logger: " + cacheService.getClass().getSimpleName());
		}

		if (useJsonPath) {
			// set it before runtime
			DefaultJsonPathWrapper djpw = null;
			try {
				djpw = new DefaultJsonPathWrapper(this.mapper);
				getLogger().debug("******** initialized new jsonPath defaults: " + djpw.getJsonPathDefault());
			} catch (Exception e) {
				throw new Exception(
						"JsonMapperService: Error instantiating " + djpw + " using useJsonPath=" + useJsonPath);
			}

		}
	}

	private void initDefaultTyping() {
		if (defaultTypeDefs != null && defaultTypeDefs.length == 2) {
			DefaultTyping defaultTyping = DefaultTyping.valueOf(defaultTypeDefs[0]);
			mapper.enableDefaultTypingAsProperty(defaultTyping, defaultTypeDefs[1]);
			getLogger().info("default typing is " + defaultTypeDefs[0] + " with key:" + defaultTypeDefs[1]);
		}
	}

	private void initFeatures() throws Exception {
		if (features != null && !features.isEmpty()) {
			
			for (Entry<String, Boolean> entry : features.entrySet()) {
				String featureKey = entry.getKey();
				Boolean featureValue = entry.getValue();	
				String featureType = featureTypes.get(featureKey);
				Class<?> configFeature = null;
				try {
					getLogger().debug("initializing featureType:  " + featureType);
					configFeature = Class.forName(featureType);
				} catch (Exception e) {
					throw new Exception("JsonMapperService: Error instantiating " + featureType + " for " + featureKey,
							e);
				}
				ConfigFeature feature = null;
				if (!StringUtils.isEmpty(featureKey) && featureValue != null) {
					try {
						if (configFeature.equals(SerializationFeature.class)) {
							feature = SerializationFeature.valueOf(featureKey);
							mapper.configure((SerializationFeature) feature, featureValue);
							assert mapper.getSerializationConfig()
									.isEnabled((SerializationFeature) feature) == featureValue;
							getLogger().info("initialized serconfig mapper feature: " + feature + " with "
									+ mapper.getSerializationConfig().isEnabled((SerializationFeature) feature));
						} else if (configFeature.equals(DeserializationFeature.class)) {
							feature = DeserializationFeature.valueOf(featureKey);
							mapper.configure((DeserializationFeature) feature, featureValue);
							assert mapper.getDeserializationConfig()
									.isEnabled((DeserializationFeature) feature) == featureValue;
							getLogger().info("initialized deserconfig mapper feature: " + feature + " with "
									+ mapper.getDeserializationConfig().isEnabled((DeserializationFeature) feature));
						} else if (configFeature.equals(MapperFeature.class)) {
							feature = MapperFeature.valueOf(featureKey);
							mapper.configure((MapperFeature) feature, featureValue);
							assert mapper.getDeserializationConfig().isEnabled((MapperFeature) feature) == featureValue;
							assert mapper.getSerializationConfig().isEnabled((MapperFeature) feature) == featureValue;
							getLogger().info("initialized serconfig mapper feature: " + feature + " with "
									+ mapper.getDeserializationConfig().isEnabled((MapperFeature) feature));
							getLogger().info("initialized deserconfig mapper feature: " + feature + " with "
									+ mapper.getSerializationConfig().isEnabled((MapperFeature) feature));
						} else if (configFeature.equals(JsonParser.class)) {
							Feature parserFeature = JsonParser.Feature.valueOf(featureKey);
							getLogger().info("initializing parser feature: " + parserFeature + " with " + featureValue);
							mapper.configure(parserFeature, featureValue);
						} else if (configFeature.equals(JsonGenerator.class)) {
							com.fasterxml.jackson.core.JsonGenerator.Feature genFeature = JsonGenerator.Feature
									.valueOf(featureKey);
							getLogger().info("initializing parser feature: " + genFeature + " with " + featureValue);
							mapper.configure(genFeature, featureValue);
						}
					} catch (Exception e) {
						throw new Exception("JsonMapperService: Error instantiating feature " + featureKey + " with  "
								+ featureValue, e);
					}
				}
			}
		}
	}

	private void initAnnotationInspectors() throws Exception {
		for (Entry<String, String> entry : annotationInspectors.entrySet()) {
			String key = entry.getKey();
			String avClass = entry.getValue();
			if (key.equals("primary") && !StringUtils.isEmpty(avClass)) {
				try {
					primary = (AnnotationIntrospector) Class.forName(avClass).getConstructor().newInstance();
				} catch (Exception e) {
					throw new Exception("JsonMapperService: Error instantiating " + avClass + " for " + key);
				}
			} else if (key.equals("secondary") && avClass != null) {
				try {
					secondary = (AnnotationIntrospector) Class.forName(avClass).getConstructor().newInstance();
				} catch (Exception e) {
					throw new Exception("JsonMapperService: Error instantiating " + avClass + " for " + key);
				}
			}
		}
		if (primary == null) {
			primary = new JacksonAnnotationIntrospector(); // support default
			getLogger().info("using default introspector:" + primary.getClass().getName());
			mapper.setAnnotationIntrospector(primary);
		} else if (primary != null && secondary != null) {
			AnnotationIntrospector pair = new AnnotationIntrospectorPair(primary, secondary);
			mapper.setAnnotationIntrospector(pair);
		} else {
			mapper.setAnnotationIntrospector(primary);
		}

		if (primary instanceof LogEnabled) {
			((LogEnabled) primary).enableLogging(getLogger().getChildLogger(primary.getClass().getSimpleName()));
			getLogger().info("setting primary introspector logger: " + primary.getClass().getSimpleName());
		}
		if (secondary instanceof LogEnabled) {
			((LogEnabled) secondary).enableLogging(getLogger().getChildLogger(secondary.getClass().getSimpleName()));
			getLogger().info("setting secondary introspector logger: " + secondary.getClass().getSimpleName());
		}
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public boolean isCacheFilters() {
		return cacheFilters;
	}

	public void setCacheFilters(boolean cacheFilters) {
		this.cacheFilters = cacheFilters;
		if (!cacheFilters)
			mapper.configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true);
	}

	static CharacterEscapes characterEscapes = new CharacterEscapes() {
		private static final long serialVersionUID = 1L;
		private final int[] asciiEscapes;
		{ // instance init
			int[] esc = standardAsciiEscapesForJSON();
			// this avoids to get evaluated immediately
			esc['<'] = CharacterEscapes.ESCAPE_STANDARD;
			esc['>'] = CharacterEscapes.ESCAPE_STANDARD;
			esc['&'] = CharacterEscapes.ESCAPE_STANDARD;
			esc['\''] = CharacterEscapes.ESCAPE_STANDARD;
			// esc['/'] = '/'; //CharacterEscapes.ESCAPE_CUSTOM;
			asciiEscapes = esc;
		}

		@Override
		public int[] getEscapeCodesForAscii() {
			return asciiEscapes;
		}

		@Override
		public SerializableString getEscapeSequence(final int ch) {
//            if ( ch == '/') { 
//                return new SerializedString("\\\\/");
//            } else {
			return null;
//            }
		}
	};
}
